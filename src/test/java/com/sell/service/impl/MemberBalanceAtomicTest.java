package com.sell.service.impl;

import com.sell.dataobject.BalanceRecord;
import com.sell.dataobject.Member;
import com.sell.dataobject.PointsRecord;
import com.sell.exception.SellException;
import com.sell.repository.MemberRepository;
import com.sell.service.MemberService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * 会员充值/余额/积分 — 原子写入(并发安全, 不丢账)回归.
 * 储值模型(喜茶/美团那类): 充值=实付+赠送一起加到余额, 消费原子扣, 调整不可为负.
 * 重点守住: 充值/调余额/调积分都走原子 SQL UPDATE(对照 deductBalance), 而非读-改-写.
 * 事务测试, 结束回滚.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberBalanceAtomicTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    private Integer newMember() {
        Member m = new Member();
        m.setPhone("bal_" + System.nanoTime());
        m.setBalance(BigDecimal.ZERO);
        m.setPoints(0);
        return memberRepository.save(m).getMemberId();
    }

    private BigDecimal balance(Integer id) {
        return memberRepository.findById(id).get().getBalance();
    }

    private Integer points(Integer id) {
        return memberRepository.findById(id).get().getPoints();
    }

    // ===== 仓储层原子方法 =====

    @Test
    public void addBalance_increments() {
        Integer id = newMember();
        assertEquals(1, memberRepository.addBalance(id, new BigDecimal("120.00"), new Date()));
        assertEquals(0, new BigDecimal("120.00").compareTo(balance(id)));
    }

    @Test
    public void adjustBalanceAtomic_rejectsNegativeResult() {
        Integer id = newMember();
        memberRepository.addBalance(id, new BigDecimal("50"), new Date());
        // -100 会变负 → 拒绝(0 行), 余额不变
        assertEquals(0, memberRepository.adjustBalanceAtomic(id, new BigDecimal("-100"), new Date()));
        assertEquals(0, new BigDecimal("50").compareTo(balance(id)));
        // -30 合法 → 20
        assertEquals(1, memberRepository.adjustBalanceAtomic(id, new BigDecimal("-30"), new Date()));
        assertEquals(0, new BigDecimal("20").compareTo(balance(id)));
    }

    @Test
    public void adjustPointsAtomic_rejectsNegativeResult() {
        Integer id = newMember();
        memberRepository.adjustPointsAtomic(id, 100, new Date());
        assertEquals(0, memberRepository.adjustPointsAtomic(id, -200, new Date())); // 会变负, 拒绝
        assertEquals(Integer.valueOf(100), points(id));
        assertEquals(1, memberRepository.adjustPointsAtomic(id, -30, new Date()));
        assertEquals(Integer.valueOf(70), points(id));
    }

    // ===== 服务层 =====

    @Test
    public void recharge_creditsPayPlusGive() {
        Integer id = newMember();
        BalanceRecord r = memberService.recharge(id, new BigDecimal("100"), new BigDecimal("20"), "boss", "充100送20");
        assertEquals(0, new BigDecimal("120").compareTo(balance(id)));
        assertEquals(0, new BigDecimal("120").compareTo(r.getAmount()));   // 到账=实付+赠送
        assertEquals(0, new BigDecimal("120").compareTo(r.getBalanceAfter()));
    }

    @Test
    public void recharge_thenAdjust_balanceAfterReflectsLatest() {
        Integer id = newMember();
        memberService.recharge(id, new BigDecimal("100"), BigDecimal.ZERO, "boss", null);
        BalanceRecord r = memberService.adjustBalance(id, new BigDecimal("-40"), "boss", "纠错");
        assertEquals(0, new BigDecimal("60").compareTo(balance(id)));
        assertEquals(0, new BigDecimal("60").compareTo(r.getBalanceAfter()));
    }

    @Test(expected = SellException.class)
    public void adjustBalance_negativeBeyondBalance_rejected() {
        Integer id = newMember();
        memberService.recharge(id, new BigDecimal("30"), BigDecimal.ZERO, "boss", null);
        memberService.adjustBalance(id, new BigDecimal("-100"), "boss", "超额扣");
    }

    @Test(expected = SellException.class)
    public void adjustPoints_negativeBeyond_rejected() {
        Integer id = newMember();
        memberService.adjustPoints(id, 10, 4, null, "送积分");
        memberService.adjustPoints(id, -50, 4, null, "超额扣");
    }

    @Test
    public void adjustPoints_writesRecordWithLatestAfter() {
        Integer id = newMember();
        memberService.adjustPoints(id, 100, 1, null, "活动赠送");
        PointsRecord r = memberService.adjustPoints(id, -30, 4, null, "兑换");
        assertEquals(Integer.valueOf(70), points(id));
        assertEquals(Integer.valueOf(70), r.getPointsAfter());
    }
}

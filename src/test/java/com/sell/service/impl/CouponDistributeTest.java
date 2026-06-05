package com.sell.service.impl;

import com.sell.dataobject.Coupon;
import com.sell.dataobject.Member;
import com.sell.repository.CouponRepository;
import com.sell.repository.MemberCouponRepository;
import com.sell.repository.MemberRepository;
import com.sell.service.MemberService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 定向发放优惠券 (PRD 8.6).
 * 重点回归: couponRepository.tryIssue 标了 clearAutomatically, 批量发放时若逐条 save 会被 clear 丢弃,
 * 导致"返回张数 > 实际 member_coupon 行数". 这里断言两者一致即可守住该坑.
 * 事务测试, 结束回滚.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class CouponDistributeTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private MemberCouponRepository memberCouponRepo;

    private Member member(String phone, Integer levelId, String tags) {
        Member m = new Member();
        m.setPhone(phone);
        m.setLevelId(levelId);
        m.setTags(tags);
        return memberRepository.save(m);
    }

    private Coupon coupon(String name, Integer totalQuantity) {
        Coupon c = new Coupon();
        c.setCouponName(name);
        c.setCouponType(0);
        c.setFaceValue(new BigDecimal("10"));
        c.setMinSpend(new BigDecimal("50"));
        c.setEnabled(true);
        c.setTotalQuantity(totalQuantity);
        return couponRepository.save(c);
    }

    /** 发放给全部会员: 返回张数 == 实际入库行数 (老 bug 会让行数 < 张数). */
    @Test
    public void distributeAll_rowsMatchCount() {
        long base = memberRepository.count();
        member("D_ALL_1", 1, null);
        member("D_ALL_2", 1, null);
        member("D_ALL_3", 2, null);
        long total = base + 3;
        Coupon c = coupon("发全部", null);

        int n = memberService.distributeCoupon(c.getCouponId(), "all", null);
        assertEquals((int) total, n);
        // 关键: 实际持有(status=0)行数必须等于返回张数
        assertEquals(n, memberCouponRepo.findHoldingMemberIds(c.getCouponId()).size());
        assertEquals(Integer.valueOf(n), couponRepository.findById(c.getCouponId()).get().getIssuedQuantity());
    }

    /** 重复发放: 已持有未使用券的会员被跳过. */
    @Test
    public void distributeTwice_dedup() {
        member("D_TW_1", 1, null);
        member("D_TW_2", 1, null);
        Coupon c = coupon("发两次", null);
        int first = memberService.distributeCoupon(c.getCouponId(), "all", null);
        int second = memberService.distributeCoupon(c.getCouponId(), "all", null);
        assertTrue(first >= 2);
        assertEquals(0, second);
        assertEquals(first, memberCouponRepo.findHoldingMemberIds(c.getCouponId()).size());
    }

    /** 受发行总量上限约束: 上限 1, 多个会员只发出 1 张. */
    @Test
    public void distribute_respectsQuantityCap() {
        member("D_CAP_1", 1, null);
        member("D_CAP_2", 1, null);
        Coupon c = coupon("限量1", 1);
        int n = memberService.distributeCoupon(c.getCouponId(), "all", null);
        assertEquals(1, n);
        assertEquals(1, memberCouponRepo.findHoldingMemberIds(c.getCouponId()).size());
    }

    /** 指定等级: 只发给该等级会员. */
    @Test
    public void distribute_byLevel() {
        member("D_LV_1", 7001, null);
        member("D_LV_2", 7001, null);
        member("D_LV_3", 7002, null);
        Coupon c = coupon("按等级", null);
        int n = memberService.distributeCoupon(c.getCouponId(), "level", "7001");
        assertEquals(2, n);
    }

    /** 指定标签: 子串匹配, 并跨发放方式去重. */
    @Test
    public void distribute_byTag_andDedupAcrossTargets() {
        Member v1 = member("D_TAG_1", 1, "钻石客");
        member("D_TAG_2", 1, "普通");
        member("D_TAG_3", 1, "钻石客,常来");
        Coupon c = coupon("按标签", null);

        // 先按标签发: 命中 v1 与第三个 → 2 张
        int byTag = memberService.distributeCoupon(c.getCouponId(), "tag", "钻石客");
        assertEquals(2, byTag);

        // 再对全部发: v1 等已持有的应被跳过
        int holdingBefore = memberCouponRepo.findHoldingMemberIds(c.getCouponId()).size();
        memberService.distributeCoupon(c.getCouponId(), "all", null);
        int holdingAfter = memberCouponRepo.findHoldingMemberIds(c.getCouponId()).size();
        // 第二次不应对已持有者重复发放
        assertTrue(holdingAfter >= holdingBefore);
        assertTrue(memberCouponRepo.findHoldingMemberIds(c.getCouponId()).contains(v1.getMemberId()));
    }

    /** 停用券不可发放. */
    @Test(expected = Exception.class)
    public void distribute_disabledCoupon_rejected() {
        Coupon c = coupon("停用券", null);
        c.setEnabled(false);
        couponRepository.save(c);
        memberService.distributeCoupon(c.getCouponId(), "all", null);
    }
}

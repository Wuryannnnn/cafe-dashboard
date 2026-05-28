package com.sell.service.impl;

import com.sell.dataobject.BalanceRecord;
import com.sell.dataobject.Coupon;
import com.sell.dataobject.Member;
import com.sell.dataobject.MemberCoupon;
import com.sell.dataobject.MemberLevel;
import com.sell.dataobject.PointsRecord;
import com.sell.exception.SellException;
import com.sell.repository.BalanceRecordRepository;
import com.sell.repository.CouponRepository;
import com.sell.repository.MemberCouponRepository;
import com.sell.repository.MemberLevelRepository;
import com.sell.repository.MemberRepository;
import com.sell.repository.PointsRecordRepository;
import com.sell.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberLevelRepository levelRepository;
    @Autowired private BalanceRecordRepository balanceRepo;
    @Autowired private PointsRecordRepository pointsRepo;
    @Autowired private MemberCouponRepository memberCouponRepo;
    @Autowired private CouponRepository couponRepository;

    @Override
    public Member findOne(Integer memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId).orElse(null);
    }

    @Override
    public Member findByPhone(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        return memberRepository.findByPhone(phone);
    }

    @Override
    public Member save(Member member) {
        Date now = new Date();
        if (member.getRegisterTime() == null) member.setRegisterTime(now);
        member.setUpdateTime(now);
        if (member.getBalance() == null) member.setBalance(BigDecimal.ZERO);
        if (member.getPoints() == null) member.setPoints(0);
        if (member.getTotalSpend() == null) member.setTotalSpend(BigDecimal.ZERO);
        if (member.getSpendCount() == null) member.setSpendCount(0);
        return memberRepository.save(member);
    }

    @Override
    public Page<Member> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return memberRepository.findAll(pageable);
        }
        return memberRepository.findByPhoneContainingOrNicknameContaining(keyword, keyword, pageable);
    }

    @Override
    public void delete(Integer memberId) {
        memberRepository.deleteById(memberId);
    }

    @Override
    @Transactional
    public BalanceRecord recharge(Integer memberId, BigDecimal payAmount, BigDecimal giveAmount, String operator, String remark) {
        Member m = findOrThrow(memberId);
        BigDecimal credit = (payAmount == null ? BigDecimal.ZERO : payAmount)
                .add(giveAmount == null ? BigDecimal.ZERO : giveAmount);
        if (credit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SellException(1, "充值金额必须 > 0");
        }
        m.setBalance(m.getBalance().add(credit));
        m.setUpdateTime(new Date());
        memberRepository.save(m);

        BalanceRecord r = new BalanceRecord();
        r.setMemberId(memberId);
        r.setRecordType(1);
        r.setAmount(credit);
        r.setBalanceAfter(m.getBalance());
        r.setOperator(operator);
        r.setRemark(remark != null ? remark : "充值"
                + (giveAmount != null && giveAmount.signum() > 0 ? " (含赠送 " + giveAmount + ")" : ""));
        r.setCreateTime(new Date());
        return balanceRepo.save(r);
    }

    @Override
    @Transactional
    public BalanceRecord adjustBalance(Integer memberId, BigDecimal delta, String operator, String remark) {
        Member m = findOrThrow(memberId);
        BigDecimal newBal = m.getBalance().add(delta);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) {
            throw new SellException(1, "调整后余额不能小于 0");
        }
        m.setBalance(newBal);
        m.setUpdateTime(new Date());
        memberRepository.save(m);

        BalanceRecord r = new BalanceRecord();
        r.setMemberId(memberId);
        r.setRecordType(4);
        r.setAmount(delta);
        r.setBalanceAfter(newBal);
        r.setOperator(operator);
        r.setRemark(remark);
        r.setCreateTime(new Date());
        return balanceRepo.save(r);
    }

    @Override
    @Transactional
    public BalanceRecord consume(Integer memberId, BigDecimal amount, String orderId, String remark) {
        if (amount == null || amount.signum() <= 0) {
            throw new SellException(1, "消费金额必须 > 0");
        }
        if (memberId == null || !memberRepository.existsById(memberId)) {
            throw new SellException(1, "会员不存在");
        }
        // 原子扣减: WHERE balance >= amount, 并发下不会双花/扣成负数
        int updated = memberRepository.deductBalance(memberId, amount, new Date());
        if (updated == 0) {
            throw new SellException(1, "余额不足");
        }
        // 扣减后重新加载(clearAutomatically 保证读到最新余额), 评估等级并记账
        Member m = findOrThrow(memberId);
        evaluateLevel(m);

        BalanceRecord r = new BalanceRecord();
        r.setMemberId(memberId);
        r.setRecordType(2);
        r.setAmount(amount.negate());
        r.setBalanceAfter(m.getBalance());
        r.setOrderId(orderId);
        r.setRemark(remark);
        r.setCreateTime(new Date());
        return balanceRepo.save(r);
    }

    @Override
    @Transactional
    public PointsRecord adjustPoints(Integer memberId, Integer delta, Integer recordType, String orderId, String remark) {
        Member m = findOrThrow(memberId);
        int newPts = m.getPoints() + (delta == null ? 0 : delta);
        if (newPts < 0) {
            throw new SellException(1, "积分扣减后不能 < 0");
        }
        m.setPoints(newPts);
        m.setUpdateTime(new Date());
        memberRepository.save(m);

        PointsRecord r = new PointsRecord();
        r.setMemberId(memberId);
        r.setRecordType(recordType != null ? recordType : 4);
        r.setPoints(delta);
        r.setPointsAfter(newPts);
        r.setOrderId(orderId);
        r.setRemark(remark);
        r.setCreateTime(new Date());
        return pointsRepo.save(r);
    }

    @Override
    public List<BalanceRecord> balanceHistory(Integer memberId) {
        return balanceRepo.findByMemberIdOrderByRecordIdDesc(memberId);
    }

    @Override
    public List<PointsRecord> pointsHistory(Integer memberId) {
        return pointsRepo.findByMemberIdOrderByRecordIdDesc(memberId);
    }

    @Override
    public List<MemberCoupon> couponHistory(Integer memberId) {
        return memberCouponRepo.findByMemberIdOrderByMemberCouponIdDesc(memberId);
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> m = new HashMap<>();
        long total = memberRepository.count();
        Date weekStart = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
        long newWeek = memberRepository.findAll().stream()
                .filter(x -> x.getRegisterTime() != null && x.getRegisterTime().after(weekStart))
                .count();
        long active = memberRepository.findAll().stream()
                .filter(x -> x.getSpendCount() != null && x.getSpendCount() > 0)
                .count();
        BigDecimal totalBalance = memberRepository.findAll().stream()
                .map(x -> x.getBalance() == null ? BigDecimal.ZERO : x.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("totalMembers", total);
        m.put("newMembersThisWeek", newWeek);
        m.put("activeMembers", active);
        m.put("totalBalance", totalBalance);
        return m;
    }

    @Override
    @Transactional
    public void evaluateLevel(Member member) {
        // 找出符合条件的最高等级
        List<MemberLevel> levels = levelRepository.findAllByOrderBySortOrderAscLevelIdAsc();
        MemberLevel best = null;
        for (MemberLevel l : levels) {
            boolean amountOk = (l.getUpgradeAmount() == null)
                    || (member.getTotalSpend() != null && member.getTotalSpend().compareTo(l.getUpgradeAmount()) >= 0);
            boolean countOk = (l.getUpgradeCount() == null)
                    || (member.getSpendCount() != null && member.getSpendCount() >= l.getUpgradeCount());
            if (amountOk && countOk) {
                if (best == null || (l.getSortOrder() != null && best.getSortOrder() != null && l.getSortOrder() > best.getSortOrder())) {
                    best = l;
                }
            }
        }
        if (best != null && (member.getLevelId() == null || !member.getLevelId().equals(best.getLevelId()))) {
            member.setLevelId(best.getLevelId());
            memberRepository.save(member);
        }
    }

    @Override
    @Transactional
    public MemberCoupon issueCoupon(Integer memberId, Integer couponId) {
        findOrThrow(memberId);
        Coupon c = couponRepository.findById(couponId).orElseThrow(() -> new SellException(1, "券不存在"));

        if (Boolean.FALSE.equals(c.getEnabled())) {
            throw new SellException(1, "优惠券已停用");
        }
        Date now = new Date();
        if (c.getValidFrom() != null && now.before(c.getValidFrom())) {
            throw new SellException(1, "优惠券未到生效时间");
        }
        if (c.getValidTo() != null && now.after(c.getValidTo())) {
            throw new SellException(1, "优惠券已过期");
        }
        // 原子占用一个发行额度(尊重 totalQuantity 上限, 并发安全); 0=已发完
        if (couponRepository.tryIssue(couponId, now) == 0) {
            throw new SellException(1, "优惠券已发完");
        }

        MemberCoupon mc = new MemberCoupon();
        mc.setMemberId(memberId);
        mc.setCouponId(couponId);
        mc.setStatus(0);
        mc.setObtainedTime(now);
        return memberCouponRepo.save(mc);
    }

    private Member findOrThrow(Integer memberId) {
        Member m = findOne(memberId);
        if (m == null) throw new SellException(1, "会员不存在");
        return m;
    }
}

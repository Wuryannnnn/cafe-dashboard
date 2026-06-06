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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (memberId == null || !memberRepository.existsById(memberId)) {
            throw new SellException(1, "会员不存在");
        }
        BigDecimal credit = (payAmount == null ? BigDecimal.ZERO : payAmount)
                .add(giveAmount == null ? BigDecimal.ZERO : giveAmount);
        if (credit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SellException(1, "充值金额必须 > 0");
        }
        // 原子加余额(并发安全, 不丢账); clearAutomatically 保证随后读到最新余额
        memberRepository.addBalance(memberId, credit, new Date());
        Member m = findOrThrow(memberId);

        BalanceRecord r = new BalanceRecord();
        r.setMemberId(memberId);
        r.setRecordType(1);
        r.setAmount(credit);
        r.setBalanceAfter(m.getBalance());
        r.setOperator(operator);
        // 修正运算符优先级: 之前 ?: 与 + 结合导致自定义备注会丢掉赠送说明
        String giveNote = (giveAmount != null && giveAmount.signum() > 0) ? " (含赠送 " + giveAmount + ")" : "";
        r.setRemark((remark != null ? remark : "充值") + giveNote);
        r.setCreateTime(new Date());
        return balanceRepo.save(r);
    }

    @Override
    @Transactional
    public BalanceRecord adjustBalance(Integer memberId, BigDecimal delta, String operator, String remark) {
        if (memberId == null || !memberRepository.existsById(memberId)) {
            throw new SellException(1, "会员不存在");
        }
        if (delta == null) {
            throw new SellException(1, "调整金额不能为空");
        }
        // 原子调整(并发安全, 防丢失更新), 调整后不能为负
        if (memberRepository.adjustBalanceAtomic(memberId, delta, new Date()) == 0) {
            throw new SellException(1, "调整后余额不能小于 0");
        }
        Member m = findOrThrow(memberId);

        BalanceRecord r = new BalanceRecord();
        r.setMemberId(memberId);
        r.setRecordType(4);
        r.setAmount(delta);
        r.setBalanceAfter(m.getBalance());
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
        if (memberId == null || !memberRepository.existsById(memberId)) {
            throw new SellException(1, "会员不存在");
        }
        int d = (delta == null ? 0 : delta);
        // 原子调整(并发安全, 防丢失更新), 调整后不能为负
        if (memberRepository.adjustPointsAtomic(memberId, d, new Date()) == 0) {
            throw new SellException(1, "积分扣减后不能 < 0");
        }
        Member m = findOrThrow(memberId);

        PointsRecord r = new PointsRecord();
        r.setMemberId(memberId);
        r.setRecordType(recordType != null ? recordType : 4);
        r.setPoints(d);
        r.setPointsAfter(m.getPoints());
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
                // null 安全比较(空 sortOrder 视为 0), 不再因某一档 sortOrder 为空而漏选更高档
                if (best == null || sortVal(l) >= sortVal(best)) {
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

    @Override
    @Transactional
    public int distributeCoupon(Integer couponId, String target, String value) {
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

        // 选定发放对象
        List<Member> targets;
        String t = target == null ? "all" : target.trim().toLowerCase();
        if ("level".equals(t)) {
            if (value == null || value.trim().isEmpty()) throw new SellException(1, "请选择会员等级");
            Integer levelId;
            try { levelId = Integer.valueOf(value.trim()); }
            catch (NumberFormatException e) { throw new SellException(1, "会员等级不合法"); }
            targets = memberRepository.findByLevelId(levelId);
        } else if ("tag".equals(t)) {
            if (value == null || value.trim().isEmpty()) throw new SellException(1, "请填写标签");
            targets = memberRepository.findByTagLike(value.trim());
        } else {
            targets = memberRepository.findAll();
        }
        if (targets.isEmpty()) {
            throw new SellException(1, "没有匹配的会员");
        }

        // 去重: 已持有该券未使用的会员跳过, 避免重复发放
        Set<Integer> holding = new HashSet<>(memberCouponRepo.findHoldingMemberIds(couponId));

        // 注意: couponRepository.tryIssue 标了 clearAutomatically, 每次调用会清空持久化上下文.
        // 若在循环内逐个 save(mc), 未 flush 的插入会被下一次 tryIssue 的 clear 丢弃(只剩最后一条).
        // 因此先把待发券收集到普通 List(瞬态对象不受 clear 影响), 循环结束后一次性 saveAll.
        List<MemberCoupon> toSave = new ArrayList<>();
        for (Member m : targets) {
            if (holding.contains(m.getMemberId())) continue;
            // 原子占用一个发行额度(尊重 totalQuantity 上限, 并发安全); 0=已发完 → 停止
            if (couponRepository.tryIssue(couponId, now) == 0) break;
            MemberCoupon mc = new MemberCoupon();
            mc.setMemberId(m.getMemberId());
            mc.setCouponId(couponId);
            mc.setStatus(0);
            mc.setObtainedTime(now);
            toSave.add(mc);
        }
        memberCouponRepo.saveAll(toSave);
        return toSave.size();
    }

    private Member findOrThrow(Integer memberId) {
        Member m = findOne(memberId);
        if (m == null) throw new SellException(1, "会员不存在");
        return m;
    }

    /** 等级排序值, null 视为 0. */
    private int sortVal(MemberLevel l) {
        return l.getSortOrder() == null ? 0 : l.getSortOrder();
    }
}

package com.sell.controller;

import com.sell.dataobject.Coupon;
import com.sell.dataobject.Member;
import com.sell.dataobject.MemberLevel;
import com.sell.dataobject.RechargePlan;
import com.sell.exception.SellException;
import com.sell.service.CouponService;
import com.sell.service.MemberService;
import com.sell.service.RestaurantTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 会员管理 - PRD 8 */
@Controller
@RequestMapping("/seller/member")
public class SellerMemberController {

    @Autowired private MemberService memberService;
    @Autowired private CouponService couponService;
    @Autowired private com.sell.repository.MemberLevelRepository levelRepo;
    @Autowired private com.sell.repository.RechargePlanRepository planRepo;

    @GetMapping("/list")
    public ModelAndView list(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "page", defaultValue = "1") Integer page,
                             @RequestParam(value = "size", defaultValue = "20") Integer size,
                             Map<String, Object> map) {
        Page<Member> result = memberService.search(keyword,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "memberId")));
        Map<Integer, String> levelNameMap = new HashMap<>();
        for (MemberLevel l : levelRepo.findAllByOrderBySortOrderAscLevelIdAsc()) {
            levelNameMap.put(l.getLevelId(), l.getLevelName());
        }
        map.put("memberPage", result);
        map.put("currentPage", page);
        map.put("size", size);
        map.put("keyword", keyword);
        map.put("levelNameMap", levelNameMap);
        map.put("overview", memberService.overview());
        return new ModelAndView("member/list", map);
    }

    @GetMapping("/index")
    public ModelAndView index(@RequestParam(value = "memberId", required = false) Integer memberId,
                              Map<String, Object> map) {
        if (memberId != null) {
            Member m = memberService.findOne(memberId);
            map.put("member", m);
            map.put("balanceRecords", memberService.balanceHistory(memberId));
            map.put("pointsRecords", memberService.pointsHistory(memberId));
            map.put("couponRecords", memberService.couponHistory(memberId));
        }
        map.put("levels", levelRepo.findAllByOrderBySortOrderAscLevelIdAsc());
        map.put("plans", planRepo.findByEnabledTrueOrderBySortOrderAsc());
        map.put("coupons", couponService.findEnabled());
        return new ModelAndView("member/index", map);
    }

    @PostMapping("/save")
    public ModelAndView save(Member form, Map<String, Object> map) {
        try {
            Member m = form.getMemberId() != null ? memberService.findOne(form.getMemberId()) : new Member();
            if (m == null) m = new Member();
            m.setPhone(form.getPhone());
            m.setNickname(form.getNickname());
            m.setOpenid(form.getOpenid());
            if (form.getLevelId() != null) m.setLevelId(form.getLevelId());
            m.setTags(form.getTags());
            m.setRemark(form.getRemark());
            m.setChannel(form.getChannel());
            memberService.save(m);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/member/list");
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/member/list");
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/recharge")
    public ModelAndView recharge(@RequestParam("memberId") Integer memberId,
                                 @RequestParam(value = "planId", required = false) Integer planId,
                                 @RequestParam(value = "payAmount", required = false) BigDecimal payAmount,
                                 @RequestParam(value = "giveAmount", required = false) BigDecimal giveAmount,
                                 @RequestParam(value = "remark", required = false) String remark,
                                 Map<String, Object> map) {
        try {
            BigDecimal pay = payAmount;
            BigDecimal give = giveAmount;
            if (planId != null) {
                RechargePlan plan = planRepo.findById(planId).orElse(null);
                if (plan != null) {
                    pay = plan.getPayAmount();
                    give = plan.getGiveAmount();
                }
            }
            memberService.recharge(memberId, pay, give, "seller", remark);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/member/index?memberId=" + memberId);
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/member/index?memberId=" + memberId);
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/adjustBalance")
    public ModelAndView adjustBalance(@RequestParam("memberId") Integer memberId,
                                      @RequestParam("delta") BigDecimal delta,
                                      @RequestParam(value = "remark", required = false) String remark,
                                      Map<String, Object> map) {
        try {
            memberService.adjustBalance(memberId, delta, "seller", remark);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/member/index?memberId=" + memberId);
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/member/index?memberId=" + memberId);
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/adjustPoints")
    public ModelAndView adjustPoints(@RequestParam("memberId") Integer memberId,
                                     @RequestParam("delta") Integer delta,
                                     @RequestParam(value = "remark", required = false) String remark,
                                     Map<String, Object> map) {
        try {
            memberService.adjustPoints(memberId, delta, 4, null, remark);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/member/index?memberId=" + memberId);
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/member/index?memberId=" + memberId);
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/issueCoupon")
    public ModelAndView issueCoupon(@RequestParam("memberId") Integer memberId,
                                    @RequestParam("couponId") Integer couponId,
                                    Map<String, Object> map) {
        try {
            memberService.issueCoupon(memberId, couponId);
        } catch (SellException e) {
            map.put("msg", e.getMessage());
            map.put("url", "/sell/seller/member/index?memberId=" + memberId);
            return new ModelAndView("common/error", map);
        }
        map.put("url", "/sell/seller/member/index?memberId=" + memberId);
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam("memberId") Integer memberId, Map<String, Object> map) {
        memberService.delete(memberId);
        map.put("url", "/sell/seller/member/list");
        return new ModelAndView("common/success", map);
    }
}

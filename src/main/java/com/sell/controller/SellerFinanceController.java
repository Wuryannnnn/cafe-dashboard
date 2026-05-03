package com.sell.controller;

import com.sell.dataobject.DamageRecord;
import com.sell.dataobject.ExpenseCategory;
import com.sell.dataobject.ExpenseRecord;
import com.sell.dataobject.SettleAccount;
import com.sell.repository.DamageRecordRepository;
import com.sell.repository.ExpenseCategoryRepository;
import com.sell.repository.ExpenseRecordRepository;
import com.sell.repository.SettleAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 财务管理 - PRD 11 */
@Controller
@RequestMapping("/seller/finance")
public class SellerFinanceController {

    @Autowired private ExpenseCategoryRepository categoryRepo;
    @Autowired private ExpenseRecordRepository recordRepo;
    @Autowired private DamageRecordRepository damageRepo;
    @Autowired private SettleAccountRepository accountRepo;

    /** 收支管理. */
    @GetMapping("/expense")
    public ModelAndView expense(Map<String, Object> map) {
        map.put("categories", categoryRepo.findAllByOrderBySortOrderAscCategoryIdAsc());
        map.put("records", recordRepo.findAllByOrderByOccurDateDescRecordIdDesc());

        // 当月汇总
        Date monthStart = Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date monthEnd = Date.from(LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        BigDecimal income = BigDecimal.ZERO, expenseSum = BigDecimal.ZERO;
        for (Object[] r : recordRepo.sumByType(monthStart, monthEnd)) {
            Integer type = (Integer) r[0];
            BigDecimal amt = (BigDecimal) r[1];
            if (type != null && type == 1) income = amt; else expenseSum = amt;
        }
        map.put("monthIncome", income);
        map.put("monthExpense", expenseSum);
        map.put("monthBalance", income.subtract(expenseSum));
        return new ModelAndView("finance/expense", map);
    }

    @PostMapping("/expense/categorySave")
    public ModelAndView saveCategory(@RequestParam(value = "categoryId", required = false) Integer categoryId,
                                     @RequestParam("categoryName") String categoryName,
                                     @RequestParam(value = "categoryType", defaultValue = "0") Integer categoryType,
                                     Map<String, Object> map) {
        ExpenseCategory c = categoryId != null
                ? categoryRepo.findById(categoryId).orElse(new ExpenseCategory()) : new ExpenseCategory();
        c.setCategoryName(categoryName);
        c.setCategoryType(categoryType);
        Date now = new Date();
        if (c.getCreateTime() == null) c.setCreateTime(now);
        c.setUpdateTime(now);
        categoryRepo.save(c);
        map.put("url", "/sell/seller/finance/expense");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/expense/categoryDelete")
    public ModelAndView deleteCategory(@RequestParam("categoryId") Integer categoryId, Map<String, Object> map) {
        categoryRepo.deleteById(categoryId);
        map.put("url", "/sell/seller/finance/expense");
        return new ModelAndView("common/success", map);
    }

    @PostMapping("/expense/recordSave")
    public ModelAndView saveRecord(@RequestParam("categoryId") Integer categoryId,
                                   @RequestParam("amount") BigDecimal amount,
                                   @RequestParam("occurDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurDate,
                                   @RequestParam(value = "remark", required = false) String remark,
                                   Map<String, Object> map) {
        ExpenseCategory cat = categoryRepo.findById(categoryId).orElse(null);
        ExpenseRecord r = new ExpenseRecord();
        r.setCategoryId(categoryId);
        r.setCategoryName(cat == null ? "" : cat.getCategoryName());
        r.setRecordType(cat == null ? 0 : cat.getCategoryType());
        r.setAmount(amount);
        r.setOccurDate(Date.from(occurDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        r.setRemark(remark);
        r.setOperator("seller");
        r.setCreateTime(new Date());
        recordRepo.save(r);
        map.put("url", "/sell/seller/finance/expense");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/expense/recordDelete")
    public ModelAndView deleteRecord(@RequestParam("recordId") Long recordId, Map<String, Object> map) {
        recordRepo.deleteById(recordId);
        map.put("url", "/sell/seller/finance/expense");
        return new ModelAndView("common/success", map);
    }

    /** 报损管理. */
    @GetMapping("/damage")
    public ModelAndView damage(Map<String, Object> map) {
        map.put("records", damageRepo.findAllByOrderByOccurDateDescRecordIdDesc());
        return new ModelAndView("finance/damage", map);
    }

    @PostMapping("/damage/save")
    public ModelAndView saveDamage(@RequestParam("reason") String reason,
                                   @RequestParam("itemName") String itemName,
                                   @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                   @RequestParam("amount") BigDecimal amount,
                                   @RequestParam("occurDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurDate,
                                   @RequestParam(value = "remark", required = false) String remark,
                                   Map<String, Object> map) {
        DamageRecord d = new DamageRecord();
        d.setReason(reason);
        d.setItemName(itemName);
        d.setQuantity(quantity);
        d.setAmount(amount);
        d.setOccurDate(Date.from(occurDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        d.setRemark(remark);
        d.setOperator("seller");
        d.setCreateTime(new Date());
        damageRepo.save(d);
        map.put("url", "/sell/seller/finance/damage");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/damage/delete")
    public ModelAndView deleteDamage(@RequestParam("recordId") Long recordId, Map<String, Object> map) {
        damageRepo.deleteById(recordId);
        map.put("url", "/sell/seller/finance/damage");
        return new ModelAndView("common/success", map);
    }

    /** 结算账户. */
    @GetMapping("/account")
    public ModelAndView account(Map<String, Object> map) {
        map.put("accounts", accountRepo.findAllByOrderByAccountIdAsc());
        return new ModelAndView("finance/account", map);
    }

    @PostMapping("/account/save")
    public ModelAndView saveAccount(SettleAccount form, Map<String, Object> map) {
        SettleAccount a = form.getAccountId() != null
                ? accountRepo.findById(form.getAccountId()).orElse(new SettleAccount())
                : new SettleAccount();
        a.setAccountName(form.getAccountName());
        a.setAccountType(form.getAccountType());
        a.setAccountNo(form.getAccountNo());
        a.setHolder(form.getHolder());
        a.setRemark(form.getRemark());
        a.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        Date now = new Date();
        if (a.getCreateTime() == null) a.setCreateTime(now);
        a.setUpdateTime(now);
        accountRepo.save(a);
        map.put("url", "/sell/seller/finance/account");
        return new ModelAndView("common/success", map);
    }

    @GetMapping("/account/delete")
    public ModelAndView deleteAccount(@RequestParam("accountId") Integer accountId, Map<String, Object> map) {
        accountRepo.deleteById(accountId);
        map.put("url", "/sell/seller/finance/account");
        return new ModelAndView("common/success", map);
    }
}

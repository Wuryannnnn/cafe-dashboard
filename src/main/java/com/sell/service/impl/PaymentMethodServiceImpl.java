package com.sell.service.impl;

import com.sell.dataobject.PaymentMethod;
import com.sell.exception.SellException;
import com.sell.repository.PaymentMethodRepository;
import com.sell.service.PaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class PaymentMethodServiceImpl implements PaymentMethodService {

    @Autowired
    private PaymentMethodRepository repository;

    @Override
    public List<PaymentMethod> findAll() {
        return repository.findAllByOrderBySortOrderAscMethodIdAsc();
    }

    @Override
    public List<PaymentMethod> findEnabled() {
        return repository.findByEnabledTrueOrderBySortOrderAscMethodIdAsc();
    }

    @Override
    public PaymentMethod findOne(Integer methodId) {
        if (methodId == null) return null;
        return repository.findById(methodId).orElse(null);
    }

    @Override
    public PaymentMethod save(PaymentMethod method) {
        if (method.getMethodName() == null || method.getMethodName().trim().isEmpty()) {
            throw new SellException(1, "结账方式名称不能为空");
        }
        Date now = new Date();
        if (method.getCreateTime() == null) method.setCreateTime(now);
        method.setUpdateTime(now);
        if (method.getEnabled() == null) method.setEnabled(true);
        if (method.getIsDefault() == null) method.setIsDefault(false);
        if (method.getSortOrder() == null) method.setSortOrder(0);
        return repository.save(method);
    }

    @Override
    public void delete(Integer methodId) {
        PaymentMethod m = findOne(methodId);
        if (m == null) return; // 幂等: 不存在直接返回
        if (Boolean.TRUE.equals(m.getIsDefault())) {
            throw new SellException(1, "默认结账方式不能删除, 请先把其他方式设为默认");
        }
        repository.deleteById(methodId);
    }

    @Override
    @Transactional
    public void setDefault(Integer methodId) {
        // 先把所有的 isDefault 置 false
        for (PaymentMethod m : repository.findAll()) {
            if (Boolean.TRUE.equals(m.getIsDefault())) {
                m.setIsDefault(false);
                m.setUpdateTime(new Date());
                repository.save(m);
            }
        }
        PaymentMethod target = findOne(methodId);
        if (target == null) throw new SellException(1, "结账方式不存在");
        target.setIsDefault(true);
        target.setUpdateTime(new Date());
        repository.save(target);
    }
}

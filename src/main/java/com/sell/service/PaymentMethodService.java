package com.sell.service;

import com.sell.dataobject.PaymentMethod;

import java.util.List;

public interface PaymentMethodService {

    List<PaymentMethod> findAll();

    List<PaymentMethod> findEnabled();

    PaymentMethod findOne(Integer methodId);

    PaymentMethod save(PaymentMethod method);

    void delete(Integer methodId);

    /** 设默认. */
    void setDefault(Integer methodId);
}

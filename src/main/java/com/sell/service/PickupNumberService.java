package com.sell.service;

public interface PickupNumberService {

    /**
     * 生成今日取餐号(每天从001开始递增)
     */
    String generatePickupNumber();
}

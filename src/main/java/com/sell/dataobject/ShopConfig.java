package com.sell.dataobject;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * 店铺配置(Key-Value)
 */
@Entity
@Data
public class ShopConfig {

    @Id
    private String configKey;

    @Column(length = 4096)
    private String configValue;
}

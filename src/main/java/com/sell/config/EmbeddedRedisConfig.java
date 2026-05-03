package com.sell.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * local profile: Redis已通过autoconfigure.exclude禁用
 * 取餐号用PickupNumberLocalImpl(内存计数器)替代
 * 登录校验已跳过
 */
@Configuration
@Profile("local")
public class EmbeddedRedisConfig {
}

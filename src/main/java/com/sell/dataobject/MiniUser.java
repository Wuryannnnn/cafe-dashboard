package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.util.Date;

/** 小程序顾客身份 (与储值 Member 解耦, 仅作点单登录态). */
@Entity
@Table(name = "mini_user", indexes = { @Index(name = "idx_mu_token", columnList = "token") })
@Data
@DynamicUpdate
public class MiniUser {

    @Id
    private String openid;          // 小程序 openid, 主键

    private String sessionKey;      // 仅后端留存, 不下发前端

    private String nickname;

    private String avatar;

    private String token;           // 不透明会话 token

    private Date tokenExpireAt;     // token 过期时间

    private Date createTime;

    private Date updateTime;
}

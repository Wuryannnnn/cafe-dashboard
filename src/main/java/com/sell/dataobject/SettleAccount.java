package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/** 结算账户 - PRD 11.3 */
@Entity
@Data
@DynamicUpdate
public class SettleAccount {

    @Id
    @GeneratedValue
    private Integer accountId;

    private String accountName;

    /** 账户类型 (微信商户号/支付宝/银行卡等). */
    private String accountType;

    /** 账号. */
    private String accountNo;

    private String holder;

    private String remark;

    private Boolean enabled = true;

    private Date createTime;

    private Date updateTime;
}

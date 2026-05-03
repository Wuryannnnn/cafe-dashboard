package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/** 收支记录 - PRD 11.1 */
@Entity
@Data
@DynamicUpdate
public class ExpenseRecord {

    @Id
    @GeneratedValue
    private Long recordId;

    private Integer categoryId;

    /** 冗余分类名 (报表友好). */
    private String categoryName;

    /** 0支出 1收入 (与 category 一致). */
    private Integer recordType;

    private BigDecimal amount;

    private Date occurDate;

    private String remark;

    private String operator;

    private Date createTime;
}

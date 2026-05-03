package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/** 收支分类 - PRD 11.1 */
@Entity
@Data
@DynamicUpdate
public class ExpenseCategory {

    @Id
    @GeneratedValue
    private Integer categoryId;

    private String categoryName;

    /** 类型: 0支出 1收入. */
    private Integer categoryType = 0;

    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}

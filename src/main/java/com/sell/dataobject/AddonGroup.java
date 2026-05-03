package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/** 菜品属性组 - PRD 3.3 (温度/甜度/奶类/浓度/加料) */
@Entity
@Data
@DynamicUpdate
public class AddonGroup {

    @Id
    @GeneratedValue
    private Integer groupId;

    /** 属性组名 (温度/甜度/奶类...). */
    private String groupName;

    /** 是否必选. */
    private Boolean required = false;

    /** 是否多选 (false=单选). */
    private Boolean multipleChoice = false;

    /** 排序. */
    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}

package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/** 常用备注 - PRD 12.3 */
@Entity
@Data
@DynamicUpdate
public class CommonRemark {

    @Id
    @GeneratedValue
    private Integer remarkId;

    /** 备注文字 (如: 少冰/多冰/去冰/加浓/打包). */
    private String text;

    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;
}

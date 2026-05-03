package com.sell.dataobject;

import com.sell.enums.PrintStationEnum;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;

/**
 * 类目
 * 2017-05-07 14:30
 */
@Entity
@DynamicUpdate
@Data
public class ProductCategory {

    /** 类目id. */
    @Id
    @GeneratedValue
    private Integer categoryId;

    /** 类目名字. */
    private String categoryName;

    /** 类目编号. */
    private Integer categoryType;

    /** 出单工位: 0吧台 1后厨, 默认吧台. */
    private Integer printStation = PrintStationEnum.BAR.getCode();

    /** 显示排序, 越小越靠前. */
    private Integer sortOrder = 0;

    private Date createTime;

    private Date updateTime;

    public ProductCategory() {
    }

    public ProductCategory(String categoryName, Integer categoryType) {
        this.categoryName = categoryName;
        this.categoryType = categoryType;
    }
}

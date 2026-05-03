package com.sell.form;

import lombok.Data;

/**
 * 2017-07-23 21:43
 */
@Data
public class CategoryForm {

    private Integer categoryId;

    /** 类目名字. */
    private String categoryName;

    /** 类目编号. */
    private Integer categoryType;

    /** 出单工位: 0吧台 1后厨. */
    private Integer printStation;
}

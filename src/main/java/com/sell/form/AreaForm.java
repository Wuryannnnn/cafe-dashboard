package com.sell.form;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class AreaForm {

    private Integer areaId;

    @NotEmpty(message = "区域名不能为空")
    private String areaName;

    private Integer sortOrder;
}

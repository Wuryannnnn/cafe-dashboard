package com.sell.form;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class TableForm {

    private Integer tableId;

    @NotNull(message = "所属区域不能为空")
    private Integer areaId;

    @NotEmpty(message = "桌号不能为空")
    private String tableCode;

    private Integer seatCount;

    private Integer sortOrder;

    private Boolean enabled;
}

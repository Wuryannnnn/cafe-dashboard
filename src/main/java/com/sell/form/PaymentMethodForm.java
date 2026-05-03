package com.sell.form;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class PaymentMethodForm {

    private Integer methodId;

    @NotEmpty(message = "结账方式名称不能为空")
    private String methodName;

    private String methodCode;

    private Boolean enabled;

    private Integer sortOrder;
}

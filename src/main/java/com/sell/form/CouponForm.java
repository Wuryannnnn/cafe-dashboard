package com.sell.form;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class CouponForm {
    private Integer couponId;
    @NotEmpty(message = "券名称不能为空")
    private String couponName;
    @NotNull(message = "请选择券类型")
    private Integer couponType;
    private BigDecimal faceValue;
    private BigDecimal minSpend;
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date validFrom;
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date validTo;
    private Boolean enabled;
    private Integer totalQuantity;
}

package com.sell.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProductAddonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String addonId;

    @JsonProperty("name")
    private String addonName;

    @JsonProperty("price")
    private BigDecimal addonPrice;
}

package com.sell.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProductSkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String skuId;

    @JsonProperty("name")
    private String skuName;

    @JsonProperty("price")
    private BigDecimal skuPrice;

    @JsonProperty("stock")
    private Integer skuStock;
}

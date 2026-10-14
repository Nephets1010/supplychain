package io.github.nephets.supplychain.web.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateProductRequest {
    private String name;
    private String spec;
    private BigDecimal costPrice;
    private BigDecimal price;
    private int stockQty;

    /** 可选：同步更新商家自身的供应商名称 */
    private String supplierName;

    /** 可选：同步更新商家自身的供应链信息 */
    private String supplierInfo;
}

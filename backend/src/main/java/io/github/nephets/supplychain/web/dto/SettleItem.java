package io.github.nephets.supplychain.web.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettleItem {
    private Long productId;
    private int qty;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
}

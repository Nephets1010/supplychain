package io.github.nephets.supplychain.web.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsResponse {
    private int totalOrders;
    private int pendingOrders;
    private int approvedOrders;
    private int rejectedOrders;
    private BigDecimal totalAmount;
    private BigDecimal totalPlatformFee;
}

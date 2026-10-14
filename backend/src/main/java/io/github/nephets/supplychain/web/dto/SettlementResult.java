package io.github.nephets.supplychain.web.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResult {
    /** 订单总成交额 */
    private BigDecimal totalAmount;

    /** 平台佣金 */
    private BigDecimal platformFee;

    /** 商家本金（供货成本） */
    private BigDecimal merchantCost;

    /** 店家利润 */
    private BigDecimal shopkeeperGain;
}

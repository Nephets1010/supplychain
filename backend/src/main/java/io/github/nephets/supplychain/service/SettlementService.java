package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.web.dto.SettleItem;
import io.github.nephets.supplychain.web.dto.SettlementResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 分账计算核心。
 *
 * <p>公式（以单笔订单为例）：
 *
 * <pre>
 *   totalAmount    = Σ (qty * unitPrice)              // 订单成交额
 *   merchantCost   = Σ (qty * costPrice)              // 商家本金（供货成本）
 *   platformFee    = round(totalAmount * rate)        // 平台佣金（默认 3%）
 *   shopkeeperGain = totalAmount - merchantCost - platformFee
 * </pre>
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final BigDecimal platformRate;

    public SettlementService(
            @Value("${supplychain.split.platform-rate:0.03}") String platformRate) {
        this.platformRate = new BigDecimal(platformRate);
    }

    public SettlementResult settle(List<SettleItem> items) {
        int size = items == null ? 0 : items.size();
        log.debug("分账计算: itemCount={}, platformRate={}", size, platformRate);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (SettleItem it : items) {
            total = total.add(it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQty())));
            cost = cost.add(it.getCostPrice().multiply(BigDecimal.valueOf(it.getQty())));
        }
        BigDecimal platformFee = total.multiply(platformRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gain =
                total.subtract(cost).subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        return new SettlementResult(total, platformFee, cost, gain);
    }

    public BigDecimal getPlatformRate() {
        return platformRate;
    }
}

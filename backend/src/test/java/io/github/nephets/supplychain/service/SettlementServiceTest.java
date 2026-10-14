package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.web.dto.SettleItem;
import io.github.nephets.supplychain.web.dto.SettlementResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SettlementServiceTest {

    private final SettlementService settlementService = new SettlementService("0.03");

    private SettleItem item(int qty, String price, String cost) {
        SettleItem it = new SettleItem();
        it.setQty(qty);
        it.setUnitPrice(new BigDecimal(price));
        it.setCostPrice(new BigDecimal(cost));
        return it;
    }

    @Test
    void example_from_requirements() {
        // 商家本金 100，店家买 120，平台挣 120*3%=3.6，店家挣 120-100-3.6=16.4
        SettlementResult r = settlementService.settle(List.of(item(1, "120", "100")));
        Assertions.assertEquals(0, r.getTotalAmount().compareTo(new BigDecimal("120.00")));
        Assertions.assertEquals(0, r.getMerchantCost().compareTo(new BigDecimal("100.00")));
        Assertions.assertEquals(0, r.getPlatformFee().compareTo(new BigDecimal("3.60")));
        Assertions.assertEquals(0, r.getShopkeeperGain().compareTo(new BigDecimal("16.40")));
    }

    @Test
    void multi_qty_and_conservation() {
        SettlementResult r = settlementService.settle(List.of(item(3, "120", "80")));
        // total = 360, cost = 240, fee = 10.80, gain = 360-240-10.80 = 109.20
        Assertions.assertEquals(0, r.getTotalAmount().compareTo(new BigDecimal("360.00")));
        Assertions.assertEquals(0, r.getMerchantCost().compareTo(new BigDecimal("240.00")));
        Assertions.assertEquals(0, r.getPlatformFee().compareTo(new BigDecimal("10.80")));
        Assertions.assertEquals(0, r.getShopkeeperGain().compareTo(new BigDecimal("109.20")));
        // 守恒: total == cost + fee + gain
        BigDecimal sum = r.getMerchantCost().add(r.getPlatformFee()).add(r.getShopkeeperGain());
        Assertions.assertEquals(0, r.getTotalAmount().compareTo(sum));
    }

    @Test
    void platform_rate_is_configurable() {
        SettlementService svc = new SettlementService("0.05");
        SettlementResult r = svc.settle(List.of(item(1, "200", "100")));
        Assertions.assertEquals(0, r.getPlatformFee().compareTo(new BigDecimal("10.00")));
    }
}

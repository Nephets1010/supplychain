package io.github.nephets.supplychain.web.dto;

import io.github.nephets.supplychain.domain.OrderItem;
import io.github.nephets.supplychain.domain.PurchaseOrder;
import java.util.List;
import lombok.Data;

@Data
public class OrderDetailResponse {
    private PurchaseOrder order;
    private List<OrderItem> items;
}

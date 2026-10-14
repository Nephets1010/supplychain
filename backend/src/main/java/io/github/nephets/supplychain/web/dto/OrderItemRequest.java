package io.github.nephets.supplychain.web.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private int qty;
}

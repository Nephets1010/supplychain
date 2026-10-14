package io.github.nephets.supplychain.web.dto;

import lombok.Data;

@Data
public class OrderItemReviewRequest {
    private Long id;
    private int qty;
}

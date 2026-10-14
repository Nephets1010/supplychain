package io.github.nephets.supplychain.web.dto;

import java.util.List;
import lombok.Data;

@Data
public class ReviewRequest {
    /** APPROVED / REJECTED */
    private String status;

    /** 审核通过时可修改订单项数量 */
    private List<OrderItemReviewRequest> items;

    private String note;
}

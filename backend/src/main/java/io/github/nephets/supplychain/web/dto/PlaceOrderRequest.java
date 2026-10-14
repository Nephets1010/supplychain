package io.github.nephets.supplychain.web.dto;

import java.util.List;
import lombok.Data;

@Data
public class PlaceOrderRequest {
    /** 订单项；商家身份由所购商品推导，客户端无需传 merchantId */
    private List<OrderItemRequest> items;
}

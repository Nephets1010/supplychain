package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.common.BizException;
import io.github.nephets.supplychain.config.SecurityUtil;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.OrderItem;
import io.github.nephets.supplychain.domain.Product;
import io.github.nephets.supplychain.domain.PurchaseOrder;
import io.github.nephets.supplychain.repository.OrderItemRepository;
import io.github.nephets.supplychain.repository.ProductRepository;
import io.github.nephets.supplychain.repository.PurchaseOrderRepository;
import io.github.nephets.supplychain.web.dto.OrderDetailResponse;
import io.github.nephets.supplychain.web.dto.OrderItemRequest;
import io.github.nephets.supplychain.web.dto.OrderItemReviewRequest;
import io.github.nephets.supplychain.web.dto.SettleItem;
import io.github.nephets.supplychain.web.dto.SettlementResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SettlementService settlementService;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;

    public OrderService(
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            OrderItemRepository orderItemRepository,
            SettlementService settlementService,
            SecurityUtil securityUtil,
            AuditService auditService) {
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.settlementService = settlementService;
        this.securityUtil = securityUtil;
        this.auditService = auditService;
    }

    /** 店家下单：逐项正确扣减商家库存（@Version 乐观锁防超卖），生成待审核订单并结算。 */
    @Transactional
    public PurchaseOrder placeOrder(List<OrderItemRequest> reqItems) {
        AppUser shop = securityUtil.current();
        Long shopkeeperId = shop.getShopkeeperId();
        int size = reqItems == null ? 0 : reqItems.size();
        log.info("下单开始: shopkeeperId={}, itemCount={}", shopkeeperId, size);
        if (shopkeeperId == null) {
            throw new BizException("当前账号未关联店家");
        }
        if (reqItems == null || reqItems.isEmpty()) {
            throw new BizException("订单项不能为空");
        }
        Product first =
                productRepository
                        .findById(reqItems.get(0).getProductId())
                        .orElseThrow(
                                () -> new BizException("商品不存在: " + reqItems.get(0).getProductId()));
        Long merchantId = first.getMerchantId();

        String orderNo = "PO" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        PurchaseOrder order = new PurchaseOrder(orderNo, shopkeeperId, merchantId, "PENDING");
        purchaseOrderRepository.save(order);

        List<SettleItem> settleItems = new ArrayList<>();
        for (OrderItemRequest ri : reqItems) {
            if (ri.getQty() <= 0) {
                throw new BizException("数量必须大于 0");
            }
            Product product =
                    productRepository
                            .findById(ri.getProductId())
                            .orElseThrow(() -> new BizException("商品不存在: " + ri.getProductId()));
            if (!merchantId.equals(product.getMerchantId())) {
                throw new BizException("订单项不属于同一商家: " + ri.getProductId());
            }
            if (product.getStockQty() < ri.getQty()) {
                throw new BizException(
                        "库存不足: " + product.getName() + " 剩余 " + product.getStockQty());
            }
            product.setStockQty(product.getStockQty() - ri.getQty());
            productRepository.save(product);

            OrderItem item =
                    new OrderItem(
                            order.getId(),
                            product.getId(),
                            product.getName(),
                            ri.getQty(),
                            product.getPrice(),
                            product.getCostPrice());
            orderItemRepository.save(item);
            settleItems.add(
                    new SettleItem(
                            product.getId(),
                            ri.getQty(),
                            product.getPrice(),
                            product.getCostPrice()));
        }

        SettlementResult result = settlementService.settle(settleItems);
        order.setTotalAmount(result.getTotalAmount());
        order.setMerchantCost(result.getMerchantCost());
        order.setPlatformFee(result.getPlatformFee());
        order.setShopkeeperGain(result.getShopkeeperGain());
        purchaseOrderRepository.save(order);

        auditService.record(
                shop.getId(),
                shop.getUsername(),
                "CREATE_ORDER",
                "ORDER",
                orderNo,
                "merchantId=" + merchantId + ",amount=" + result.getTotalAmount());
        log.info("下单完成: orderNo={}, status=PENDING, amount={}", orderNo, result.getTotalAmount());
        return order;
    }

    /** 管理方审核：通过可改数量并结算；驳回则回补库存。 */
    @Transactional
    public PurchaseOrder review(
            Long orderId,
            String status,
            List<OrderItemReviewRequest> reviewItems,
            Long reviewerId,
            String reviewerName,
            String note) {
        log.info("审核订单开始: orderId={}, status={}, reviewer={}", orderId, status, reviewerName);
        PurchaseOrder order =
                purchaseOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new BizException("订单不存在"));
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException("仅待审核订单可被处理，当前状态: " + order.getStatus());
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BizException("审核状态非法，应为 APPROVED / REJECTED");
        }

        List<OrderItem> current = orderItemRepository.findByOrderId(orderId);

        if ("APPROVED".equals(status) && reviewItems != null && !reviewItems.isEmpty()) {
            for (OrderItemReviewRequest ri : reviewItems) {
                OrderItem item =
                        current.stream()
                                .filter(i -> i.getId().equals(ri.getId()))
                                .findFirst()
                                .orElseThrow(() -> new BizException("订单项不存在: " + ri.getId()));
                if (ri.getQty() <= 0) {
                    throw new BizException("数量必须大于 0");
                }
                int delta = ri.getQty() - item.getQty();
                Product product =
                        productRepository
                                .findById(item.getProductId())
                                .orElseThrow(() -> new BizException("商品已删除"));
                if (product.getStockQty() < delta) {
                    throw new BizException("库存不足，无法调整到 " + ri.getQty() + "（差 " + delta + "）");
                }
                product.setStockQty(product.getStockQty() - delta);
                productRepository.save(product);
                item.setQty(ri.getQty());
                item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(ri.getQty())));
                orderItemRepository.save(item);
            }
        }

        if ("REJECTED".equals(status)) {
            for (OrderItem item : current) {
                Product product =
                        productRepository
                                .findById(item.getProductId())
                                .orElseThrow(() -> new BizException("商品已删除"));
                product.setStockQty(product.getStockQty() + item.getQty());
                productRepository.save(product);
            }
            order.setStatus("REJECTED");
            order.setReviewedAt(Instant.now());
            order.setReviewerId(reviewerId);
            order.setReviewNote(note);
            purchaseOrderRepository.save(order);
            auditService.record(
                    reviewerId,
                    reviewerName,
                    "REJECT_ORDER",
                    "ORDER",
                    order.getOrderNo(),
                    "note=" + note);
            return order;
        }

        // APPROVED：按最终数量重新结算
        List<SettleItem> settleItems =
                orderItemRepository.findByOrderId(orderId).stream()
                        .map(
                                i ->
                                        new SettleItem(
                                                i.getProductId(),
                                                i.getQty(),
                                                i.getUnitPrice(),
                                                i.getCostPrice()))
                        .toList();
        SettlementResult result = settlementService.settle(settleItems);
        order.setTotalAmount(result.getTotalAmount());
        order.setMerchantCost(result.getMerchantCost());
        order.setPlatformFee(result.getPlatformFee());
        order.setShopkeeperGain(result.getShopkeeperGain());
        order.setStatus("APPROVED");
        order.setReviewedAt(Instant.now());
        order.setReviewerId(reviewerId);
        order.setReviewNote(note);
        purchaseOrderRepository.save(order);
        auditService.record(
                reviewerId,
                reviewerName,
                "APPROVE_ORDER",
                "ORDER",
                order.getOrderNo(),
                "amount=" + result.getTotalAmount() + ",gain=" + result.getShopkeeperGain());
        return order;
    }

    public List<PurchaseOrder> ordersByMerchant(Long merchantId) {
        return purchaseOrderRepository.findByMerchantId(merchantId);
    }

    public List<PurchaseOrder> ordersByShopkeeper(Long shopkeeperId) {
        return purchaseOrderRepository.findByShopkeeperId(shopkeeperId);
    }

    /** 管理方查看任意订单详情（不校验归属，仅限 admin 端点调用）。 */
    public OrderDetailResponse detail(Long orderId) {
        log.debug("查询订单详情(admin): orderId={}", orderId);
        PurchaseOrder order =
                purchaseOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new BizException("订单不存在"));
        return toDetailResponse(order);
    }

    /**
     * 店家查看订单详情：校验订单归属，阻断越权读取他人订单（IDOR）。
     *
     * @param orderId 订单 id
     * @param shopkeeperId 当前登录店家 id
     * @return 订单详情
     * @throws BizException 订单不存在，或订单不属于该店家
     */
    public OrderDetailResponse detailForShopkeeper(Long orderId, Long shopkeeperId) {
        log.debug("查询订单详情(shopkeeper): orderId={}, shopkeeperId={}", orderId, shopkeeperId);
        PurchaseOrder order =
                purchaseOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new BizException("订单不存在"));
        if (order.getShopkeeperId() == null || !order.getShopkeeperId().equals(shopkeeperId)) {
            throw new BizException("无权查看该订单");
        }
        return toDetailResponse(order);
    }

    private OrderDetailResponse toDetailResponse(PurchaseOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrder(order);
        response.setItems(items);
        return response;
    }
}

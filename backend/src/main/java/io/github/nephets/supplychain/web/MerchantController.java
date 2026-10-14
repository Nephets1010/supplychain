package io.github.nephets.supplychain.web;

import io.github.nephets.supplychain.config.SecurityUtil;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.Product;
import io.github.nephets.supplychain.domain.PurchaseOrder;
import io.github.nephets.supplychain.repository.OrderItemRepository;
import io.github.nephets.supplychain.service.OrderService;
import io.github.nephets.supplychain.service.ProductService;
import io.github.nephets.supplychain.web.dto.CreateProductRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private static final Logger log = LoggerFactory.getLogger(MerchantController.class);

    private final ProductService productService;
    private final OrderService orderService;
    private final SecurityUtil securityUtil;
    private final OrderItemRepository orderItemRepository;

    public MerchantController(
            ProductService productService,
            OrderService orderService,
            SecurityUtil securityUtil,
            OrderItemRepository orderItemRepository) {
        this.productService = productService;
        this.orderService = orderService;
        this.securityUtil = securityUtil;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/products")
    public List<Product> products() {
        log.info("商家查询商品列表");
        return productService.myProducts();
    }

    @PostMapping("/products")
    public Product create(@Valid @RequestBody CreateProductRequest request) {
        log.info("商家创建商品: name={}", request.getName());
        return productService.create(request);
    }

    @PutMapping("/products/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody CreateProductRequest request) {
        log.info("商家更新商品: id={}", id);
        return productService.update(id, request);
    }

    @GetMapping("/orders")
    public List<PurchaseOrder> orders() {
        AppUser user = securityUtil.current();
        log.info("商家查询订单列表: merchantId={}", user.getMerchantId());
        List<PurchaseOrder> list = orderService.ordersByMerchant(user.getMerchantId());
        for (PurchaseOrder order : list) {
            order.setItems(orderItemRepository.findByOrderId(order.getId()));
        }
        return list;
    }
}

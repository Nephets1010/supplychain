package io.github.nephets.supplychain.web;

import io.github.nephets.supplychain.config.SecurityUtil;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.Merchant;
import io.github.nephets.supplychain.domain.Product;
import io.github.nephets.supplychain.domain.PurchaseOrder;
import io.github.nephets.supplychain.repository.MerchantRepository;
import io.github.nephets.supplychain.repository.ProductRepository;
import io.github.nephets.supplychain.service.OrderService;
import io.github.nephets.supplychain.web.dto.OrderDetailResponse;
import io.github.nephets.supplychain.web.dto.PlaceOrderRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private static final Logger log = LoggerFactory.getLogger(ShopController.class);

    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final SecurityUtil securityUtil;

    public ShopController(
            MerchantRepository merchantRepository,
            ProductRepository productRepository,
            OrderService orderService,
            SecurityUtil securityUtil) {
        this.merchantRepository = merchantRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/merchants")
    public List<Merchant> merchants() {
        log.info("店家查询商家列表");
        return merchantRepository.findAll();
    }

    @GetMapping("/products")
    public List<Product> products(@RequestParam Long merchantId) {
        log.info("店家查询商家商品: merchantId={}", merchantId);
        return productRepository.findByMerchantId(merchantId);
    }

    @PostMapping("/orders")
    public PurchaseOrder placeOrder(@RequestBody PlaceOrderRequest request) {
        int size = request.getItems() == null ? 0 : request.getItems().size();
        log.info("店家下单: itemCount={}", size);
        return orderService.placeOrder(request.getItems());
    }

    @GetMapping("/orders")
    public List<PurchaseOrder> myOrders() {
        AppUser user = securityUtil.current();
        log.info("店家查询我的订单: shopkeeperId={}", user.getShopkeeperId());
        return orderService.ordersByShopkeeper(user.getShopkeeperId());
    }

    @GetMapping("/orders/{id}")
    public OrderDetailResponse orderDetail(@PathVariable Long id) {
        AppUser user = securityUtil.current();
        log.info("店家查询订单详情: id={}, shopkeeperId={}", id, user.getShopkeeperId());
        return orderService.detailForShopkeeper(id, user.getShopkeeperId());
    }
}

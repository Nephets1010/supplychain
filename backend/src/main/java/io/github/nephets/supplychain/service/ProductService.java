package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.common.BizException;
import io.github.nephets.supplychain.config.SecurityUtil;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.Merchant;
import io.github.nephets.supplychain.domain.Product;
import io.github.nephets.supplychain.repository.MerchantRepository;
import io.github.nephets.supplychain.repository.ProductRepository;
import io.github.nephets.supplychain.web.dto.CreateProductRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            MerchantRepository merchantRepository,
            SecurityUtil securityUtil,
            AuditService auditService) {
        this.productRepository = productRepository;
        this.merchantRepository = merchantRepository;
        this.securityUtil = securityUtil;
        this.auditService = auditService;
    }

    public List<Product> myProducts() {
        Long merchantId = securityUtil.current().getMerchantId();
        log.debug("查询我的商品: merchantId={}", merchantId);
        return productRepository.findByMerchantId(merchantId);
    }

    public Product create(CreateProductRequest req) {
        AppUser user = securityUtil.current();
        Long merchantId = user.getMerchantId();
        log.info("创建商品: merchantId={}, name={}", merchantId, req.getName());
        Product product =
                new Product(
                        merchantId,
                        req.getName(),
                        req.getCostPrice(),
                        req.getPrice(),
                        req.getStockQty());
        product.setSpec(req.getSpec());
        productRepository.save(product);

        if (req.getSupplierName() != null || req.getSupplierInfo() != null) {
            Merchant merchant =
                    merchantRepository
                            .findById(merchantId)
                            .orElseThrow(() -> new BizException("商家不存在"));
            if (req.getSupplierName() != null) {
                merchant.setSupplierName(req.getSupplierName());
            }
            if (req.getSupplierInfo() != null) {
                merchant.setSupplierInfo(req.getSupplierInfo());
            }
            merchantRepository.save(merchant);
        }

        auditService.record(
                user.getId(),
                user.getUsername(),
                "CREATE_PRODUCT",
                "PRODUCT",
                String.valueOf(product.getId()),
                "name=" + req.getName());
        return product;
    }

    public Product update(Long id, CreateProductRequest req) {
        AppUser user = securityUtil.current();
        log.info("更新商品: id={}, merchantId={}", id, user.getMerchantId());
        Product product =
                productRepository
                        .findByMerchantIdAndId(user.getMerchantId(), id)
                        .orElseThrow(() -> new BizException("商品不存在或无权限"));
        if (req.getName() != null) {
            product.setName(req.getName());
        }
        if (req.getSpec() != null) {
            product.setSpec(req.getSpec());
        }
        if (req.getCostPrice() != null) {
            product.setCostPrice(req.getCostPrice());
        }
        if (req.getPrice() != null) {
            product.setPrice(req.getPrice());
        }
        if (req.getStockQty() > 0) {
            product.setStockQty(req.getStockQty());
        }
        productRepository.save(product);
        auditService.record(
                user.getId(),
                user.getUsername(),
                "UPDATE_PRODUCT",
                "PRODUCT",
                String.valueOf(id),
                "name=" + product.getName());
        return product;
    }
}

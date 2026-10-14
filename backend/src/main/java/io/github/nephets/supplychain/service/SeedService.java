package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.Merchant;
import io.github.nephets.supplychain.domain.Product;
import io.github.nephets.supplychain.domain.Shopkeeper;
import io.github.nephets.supplychain.repository.AppUserRepository;
import io.github.nephets.supplychain.repository.MerchantRepository;
import io.github.nephets.supplychain.repository.ProductRepository;
import io.github.nephets.supplychain.repository.ShopkeeperRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动种子：演示账号、商家、店家与商品。仅在无 admin 账号时执行一次。
 *
 * <p>三个种子账号的口令优先从环境变量注入（未配置时使用下方默认值，行为与历史版本一致）： SEED_ADMIN_PASSWORD（默认
 * admin123）、SEED_MERCHANT_PASSWORD（默认 merch123）、 SEED_SHOP_PASSWORD（默认
 * shop123）。生产部署必须显式注入强口令，并在初始化后尽快改密。
 */
@Component
public class SeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final AppUserRepository appUserRepository;
    private final MerchantRepository merchantRepository;
    private final ShopkeeperRepository shopkeeperRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String merchantPassword;
    private final String shopPassword;

    public SeedService(
            AppUserRepository appUserRepository,
            MerchantRepository merchantRepository,
            ShopkeeperRepository shopkeeperRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SEED_ADMIN_PASSWORD:admin123}") String adminPassword,
            @Value("${SEED_MERCHANT_PASSWORD:merch123}") String merchantPassword,
            @Value("${SEED_SHOP_PASSWORD:shop123}") String shopPassword) {
        this.appUserRepository = appUserRepository;
        this.merchantRepository = merchantRepository;
        this.shopkeeperRepository = shopkeeperRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.merchantPassword = merchantPassword;
        this.shopPassword = shopPassword;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.findByUsername("admin").isPresent()) {
            log.info("种子数据已存在，跳过初始化");
            return;
        }
        log.info("开始初始化种子数据(admin/merchant/shop 演示账号与商品)");
        AppUser admin =
                appUserRepository.save(
                        new AppUser("admin", passwordEncoder.encode(adminPassword), "ADMIN"));

        Merchant merchant = new Merchant("优选货源商");
        merchant.setContact("13800000000");
        merchant.setSupplierName("华南总仓");
        merchant.setSupplierInfo("规格A/价格100/数量500");
        merchant = merchantRepository.save(merchant);

        AppUser merchantUser =
                appUserRepository.save(
                        new AppUser(
                                "merchant1", passwordEncoder.encode(merchantPassword), "MERCHANT"));
        merchantUser.setMerchantId(merchant.getId());
        appUserRepository.save(merchantUser);

        Shopkeeper shopkeeper = shopkeeperRepository.save(new Shopkeeper("星光便利店"));
        shopkeeper.setContact("13900000000");
        shopkeeper = shopkeeperRepository.save(shopkeeper);

        AppUser shopUser =
                appUserRepository.save(
                        new AppUser("shop1", passwordEncoder.encode(shopPassword), "SHOPKEEPER"));
        shopUser.setShopkeeperId(shopkeeper.getId());
        appUserRepository.save(shopUser);

        productRepository.save(
                new Product(
                        merchant.getId(),
                        "精品咖啡豆 1kg",
                        new BigDecimal("80"),
                        new BigDecimal("120"),
                        100));
        productRepository.save(
                new Product(
                        merchant.getId(),
                        "有机绿茶 250g",
                        new BigDecimal("40"),
                        new BigDecimal("65"),
                        200));

        // admin 关联仅为占位，避免空值
        admin.setMerchantId(merchant.getId());
        appUserRepository.save(admin);
        log.info("种子数据初始化完成");
    }
}

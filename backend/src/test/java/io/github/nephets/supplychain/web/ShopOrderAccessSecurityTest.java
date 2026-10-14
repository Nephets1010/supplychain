package io.github.nephets.supplychain.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.Shopkeeper;
import io.github.nephets.supplychain.repository.AppUserRepository;
import io.github.nephets.supplychain.repository.ShopkeeperRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 越权访问回归测试：店家只能读取属于自己的订单（IDOR 防护）。
 *
 * <p>覆盖两条路径：归属匹配时正常返回（行为不变），归属不匹配时拒绝访问。 同时校验管理方端点仍可读取任意订单，确保 detail(Long) 未被破坏。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShopOrderAccessSecurityTest {

    private static final String AUTH = "Authorization";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ShopkeeperRepository shopkeeperRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /** 准备第二个店家账号，用于验证跨店家越权读取被拒绝。 */
    @BeforeEach
    void ensureSecondShopkeeper() {
        if (appUserRepository.findByUsername("shop2").isEmpty()) {
            Shopkeeper other = shopkeeperRepository.save(new Shopkeeper("另一家便利店"));
            AppUser otherUser =
                    appUserRepository.save(
                            new AppUser("shop2", passwordEncoder.encode("shop123"), "SHOPKEEPER"));
            otherUser.setShopkeeperId(other.getId());
            appUserRepository.save(otherUser);
        }
    }

    private String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult r =
                mvc.perform(post("/api/auth/login").contentType(JSON).content(body))
                        .andExpect(status().isOk())
                        .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.token");
    }

    /** 以 shop1 身份下一个 1 件的订单，返回订单 id。 */
    private Long placeOrderAsShop1(String shopToken) throws Exception {
        MvcResult merchants =
                mvc.perform(get("/api/shop/merchants").header(AUTH, "Bearer " + shopToken))
                        .andExpect(status().isOk())
                        .andReturn();
        Long merchantId =
                new BigDecimal(
                                JsonPath.read(
                                                merchants
                                                        .getResponse()
                                                        .getContentAsString(StandardCharsets.UTF_8),
                                                "$[0].id")
                                        .toString())
                        .longValue();
        MvcResult products =
                mvc.perform(
                                get("/api/shop/products")
                                        .param("merchantId", String.valueOf(merchantId))
                                        .header(AUTH, "Bearer " + shopToken))
                        .andExpect(status().isOk())
                        .andReturn();
        List<Map<String, Object>> ps =
                JsonPath.read(
                        products.getResponse().getContentAsString(StandardCharsets.UTF_8), "$");
        Long productId = new BigDecimal(ps.get(0).get("id").toString()).longValue();

        String orderBody =
                "{\"merchantId\":"
                        + merchantId
                        + ",\"items\":[{\"productId\":"
                        + productId
                        + ",\"qty\":1}]}";
        MvcResult order =
                mvc.perform(
                                post("/api/shop/orders")
                                        .header(AUTH, "Bearer " + shopToken)
                                        .contentType(JSON)
                                        .content(orderBody))
                        .andExpect(status().isOk())
                        .andReturn();
        return new BigDecimal(
                        JsonPath.read(
                                        order.getResponse()
                                                .getContentAsString(StandardCharsets.UTF_8),
                                        "$.id")
                                .toString())
                .longValue();
    }

    /** 订单归属店家可正常查看详情：成功路径行为保持不变。 */
    @Test
    void owner_can_read_own_order_detail() throws Exception {
        String shopToken = login("shop1", "shop123");
        Long orderId = placeOrderAsShop1(shopToken);

        mvc.perform(get("/api/shop/orders/{id}", orderId).header(AUTH, "Bearer " + shopToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(orderId))
                .andExpect(jsonPath("$.items").isArray());
    }

    /** 非归属店家读取他人订单应被拒绝（IDOR 防护）。 */
    @Test
    void other_shopkeeper_cannot_read_order_detail() throws Exception {
        String shop1Token = login("shop1", "shop123");
        Long orderId = placeOrderAsShop1(shop1Token);
        String shop2Token = login("shop2", "shop123");

        MvcResult denied =
                mvc.perform(
                                get("/api/shop/orders/{id}", orderId)
                                        .header(AUTH, "Bearer " + shop2Token))
                        .andExpect(status().isBadRequest())
                        .andReturn();
        String body = denied.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertTrue(body.contains("无权查看该订单"), "越权访问应返回无权提示，实际响应: " + body);

        // 被拒后该订单对 shop2 的列表同样不可见
        mvc.perform(get("/api/shop/orders").header(AUTH, "Bearer " + shop2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + orderId + ")]").doesNotExist());
    }

    /** 管理方仍可读取任意订单：detail(Long) 未被本次改动破坏。 */
    @Test
    void admin_still_can_read_any_order_detail() throws Exception {
        String shopToken = login("shop1", "shop123");
        Long orderId = placeOrderAsShop1(shopToken);
        String adminToken = login("admin", "admin123");

        mvc.perform(get("/api/admin/orders/{id}", orderId).header(AUTH, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(orderId));
    }
}

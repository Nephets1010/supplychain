package io.github.nephets.supplychain.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nephets.supplychain.repository.AppUserRepository;
import io.github.nephets.supplychain.repository.ProductRepository;
import io.github.nephets.supplychain.repository.PurchaseOrderRepository;
import io.github.nephets.supplychain.security.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ProductRepository productRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;

    private String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult r =
                mvc.perform(post("/api/auth/login").contentType(JSON).content(body))
                        .andExpect(status().isOk())
                        .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    private static Long asLong(Object o) {
        return ((Integer) o).longValue();
    }

    private static BigDecimal bd(Object o) {
        return new BigDecimal(o.toString());
    }

    @Test
    void unauthenticated_is_401() throws Exception {
        mvc.perform(get("/api/shop/products").param("merchantId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shop_cannot_access_admin_endpoint_403() throws Exception {
        String token = login("shop1", "shop123");
        mvc.perform(get("/api/admin/orders").header(AUTH, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void full_flow_place_approve_reconcile() throws Exception {
        String shopToken = login("shop1", "shop123");
        String adminToken = login("admin", "admin123");

        // 选商家
        MvcResult merchants =
                mvc.perform(get("/api/shop/merchants").header(AUTH, "Bearer " + shopToken))
                        .andExpect(status().isOk())
                        .andReturn();
        Long merchantId =
                asLong(JsonPath.read(merchants.getResponse().getContentAsString(), "$[0].id"));

        // 选商品（精品咖啡豆 1kg，成本80 售价120 库存100）
        MvcResult products =
                mvc.perform(
                                get("/api/shop/products")
                                        .param("merchantId", String.valueOf(merchantId))
                                        .header(AUTH, "Bearer " + shopToken))
                        .andExpect(status().isOk())
                        .andReturn();
        List<Map<String, Object>> ps =
                JsonPath.read(products.getResponse().getContentAsString(), "$");
        Map<String, Object> coffee =
                ps.stream()
                        .filter(p -> "精品咖啡豆 1kg".equals(p.get("name")))
                        .findFirst()
                        .orElseThrow();
        Long productId = asLong(coffee.get("id"));
        int stockBefore = (Integer) coffee.get("stockQty");

        // 下单 qty=3 → 库存应扣到 97，成交额 360，本金 240，平台费 10.80，利润 109.20
        String orderBody =
                "{\"merchantId\":"
                        + merchantId
                        + ",\"items\":[{\"productId\":"
                        + productId
                        + ",\"qty\":3}]}";
        MvcResult order =
                mvc.perform(
                                post("/api/shop/orders")
                                        .header(AUTH, "Bearer " + shopToken)
                                        .contentType(JSON)
                                        .content(orderBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andExpect(jsonPath("$.totalAmount").value(360))
                        .andExpect(jsonPath("$.merchantCost").value(240))
                        .andExpect(jsonPath("$.platformFee").value(10.8))
                        .andExpect(jsonPath("$.shopkeeperGain").value(109.2))
                        .andReturn();
        Long orderId = asLong(JsonPath.read(order.getResponse().getContentAsString(), "$.id"));

        // 库存实时扣减
        int stockAfter = productRepository.findById(productId).orElseThrow().getStockQty();
        Assertions.assertEquals(stockBefore - 3, stockAfter);

        // 管理方审核通过
        String reviewBody = "{\"status\":\"APPROVED\"}";
        mvc.perform(
                        post("/api/admin/orders/{id}/review", orderId)
                                .header(AUTH, "Bearer " + adminToken)
                                .contentType(JSON)
                                .content(reviewBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 触发对账（按订单产生日 = 今天）
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        mvc.perform(
                        post("/api/admin/reconciliations/run")
                                .param("date", today)
                                .header(AUTH, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 对账记录存在且金额一致
        mvc.perform(
                        get("/api/admin/reconciliations")
                                .param("date", today)
                                .header(AUTH, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalAmount").value(360))
                .andExpect(jsonPath("$[0].platformFee").value(10.8));

        // 审计日志包含审核动作
        mvc.perform(get("/api/admin/audit").header(AUTH, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action=='APPROVE_ORDER')]").exists());
    }

    @Test
    void insufficient_stock_is_rejected() throws Exception {
        String shopToken = login("shop1", "shop123");
        MvcResult merchants =
                mvc.perform(get("/api/shop/merchants").header(AUTH, "Bearer " + shopToken))
                        .andExpect(status().isOk())
                        .andReturn();
        Long merchantId =
                asLong(JsonPath.read(merchants.getResponse().getContentAsString(), "$[0].id"));
        MvcResult products =
                mvc.perform(
                                get("/api/shop/products")
                                        .param("merchantId", String.valueOf(merchantId))
                                        .header(AUTH, "Bearer " + shopToken))
                        .andExpect(status().isOk())
                        .andReturn();
        List<Map<String, Object>> ps =
                JsonPath.read(products.getResponse().getContentAsString(), "$");
        Map<String, Object> tea =
                ps.stream()
                        .filter(p -> "有机绿茶 250g".equals(p.get("name")))
                        .findFirst()
                        .orElseThrow();
        Long productId = asLong(tea.get("id"));
        int stockBefore = (Integer) tea.get("stockQty");

        String orderBody =
                "{\"merchantId\":"
                        + merchantId
                        + ",\"items\":[{\"productId\":"
                        + productId
                        + ",\"qty\":99999}]}";
        mvc.perform(
                        post("/api/shop/orders")
                                .header(AUTH, "Bearer " + shopToken)
                                .contentType(JSON)
                                .content(orderBody))
                .andExpect(status().isBadRequest());

        // 库存未被破坏
        int stockAfter = productRepository.findById(productId).orElseThrow().getStockQty();
        Assertions.assertEquals(stockBefore, stockAfter);
    }
}

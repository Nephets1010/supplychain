package io.github.nephets.supplychain.repository;

import io.github.nephets.supplychain.domain.PurchaseOrder;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByShopkeeperId(Long shopkeeperId);

    List<PurchaseOrder> findByMerchantId(Long merchantId);

    List<PurchaseOrder> findByStatus(String status);

    List<PurchaseOrder> findByOrderNo(String orderNo);

    List<PurchaseOrder> findByStatusAndCreatedAtBetween(String status, Instant start, Instant end);
}

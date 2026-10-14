package io.github.nephets.supplychain.repository;

import io.github.nephets.supplychain.domain.Reconciliation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {
    Optional<Reconciliation> findByReconcileDateAndMerchantIdAndShopkeeperId(
            LocalDate reconcileDate, Long merchantId, Long shopkeeperId);

    List<Reconciliation> findByReconcileDate(LocalDate reconcileDate);
}

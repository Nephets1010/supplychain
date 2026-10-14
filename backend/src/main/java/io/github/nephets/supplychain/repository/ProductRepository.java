package io.github.nephets.supplychain.repository;

import io.github.nephets.supplychain.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByMerchantId(Long merchantId);

    Optional<Product> findByMerchantIdAndId(Long merchantId, Long id);
}

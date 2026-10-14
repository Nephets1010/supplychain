package io.github.nephets.supplychain.repository;

import io.github.nephets.supplychain.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {}

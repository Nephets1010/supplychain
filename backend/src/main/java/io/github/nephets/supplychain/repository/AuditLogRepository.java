package io.github.nephets.supplychain.repository;

import io.github.nephets.supplychain.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}

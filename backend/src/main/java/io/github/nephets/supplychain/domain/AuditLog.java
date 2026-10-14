package io.github.nephets.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;
    private String actorName;

    /** CREATE_ORDER / APPROVE_ORDER / REJECT_ORDER / MODIFY_ORDER / RECONCILE / LOGIN ... */
    @Column(nullable = false)
    private String action;

    private String targetType;
    private String targetId;

    @Column(length = 2000)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public AuditLog(
            Long actorId,
            String actorName,
            String action,
            String targetType,
            String targetId,
            String detail) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
    }
}

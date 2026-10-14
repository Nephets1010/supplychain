package io.github.nephets.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate reconcileDate;

    @Column(nullable = false)
    private Long merchantId;

    @Column(nullable = false)
    private Long shopkeeperId;

    private int orderCount;

    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal merchantCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal shopkeeperGain = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(nullable = false)
    private String status = "RECONCILED";

    public Reconciliation(LocalDate reconcileDate, Long merchantId, Long shopkeeperId) {
        this.reconcileDate = reconcileDate;
        this.merchantId = merchantId;
        this.shopkeeperId = shopkeeperId;
    }
}

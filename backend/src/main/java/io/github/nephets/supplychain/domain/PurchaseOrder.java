package io.github.nephets.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNo;

    @Column(nullable = false)
    private Long shopkeeperId;

    @Column(nullable = false)
    private Long merchantId;

    /** PENDING / APPROVED / REJECTED */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal merchantCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal shopkeeperGain = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant reviewedAt;
    private Long reviewerId;
    private String reviewNote;

    /** 订单项快照（仅用于商家订单列表等视图序列化，非持久化列） */
    @Transient private List<OrderItem> items;

    public PurchaseOrder(String orderNo, Long shopkeeperId, Long merchantId, String status) {
        this.orderNo = orderNo;
        this.shopkeeperId = shopkeeperId;
        this.merchantId = merchantId;
        this.status = status;
    }
}

package io.github.nephets.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    private String productName;

    @Column(nullable = false)
    private int qty;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal costPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    public OrderItem(
            Long orderId,
            Long productId,
            String productName,
            int qty,
            BigDecimal unitPrice,
            BigDecimal costPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
    }
}

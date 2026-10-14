package io.github.nephets.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long merchantId;

    @Column(nullable = false)
    private String name;

    private String spec;

    /** 商家成本（本金） */
    @Column(nullable = false)
    private BigDecimal costPrice;

    /** 店家采购单价 */
    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQty;

    /** 乐观锁，防并发超卖 */
    @Version private Long version;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Product(
            Long merchantId, String name, BigDecimal costPrice, BigDecimal price, int stockQty) {
        this.merchantId = merchantId;
        this.name = name;
        this.costPrice = costPrice;
        this.price = price;
        this.stockQty = stockQty;
    }
}

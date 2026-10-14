package io.github.nephets.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String contact;

    /** 供应商名称（商家字段：添加供应商） */
    private String supplierName;

    /** 供应链信息（规格 / 价格 / 数量等备注） */
    @Column(length = 2000)
    private String supplierInfo;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Merchant(String name) {
        this.name = name;
    }
}

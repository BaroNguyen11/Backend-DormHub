package com.dormhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prices", uniqueConstraints = @UniqueConstraint(
        name = "uk_prices_type_effective_from", columnNames = {"type", "effective_from"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prices {

    @Id
    @Column(name = "prices_id", nullable = false)
    private String pricesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PriceType type;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "note")
    private String note;

    public Prices(String pricesId, PriceType type, BigDecimal unitPrice, LocalDate effectiveFrom,
            LocalDateTime createdAt, String createdBy, String note) {
        this.pricesId = pricesId;
        this.type = type;
        this.unitPrice = unitPrice;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.note = note;
    }

}

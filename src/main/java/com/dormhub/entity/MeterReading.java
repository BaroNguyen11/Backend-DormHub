package com.dormhub.entity;

import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.config.YearMonthAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meter_readings", uniqueConstraints = @UniqueConstraint(
        name = "uk_meter_readings_room_month", columnNames = {"room_id", "reading_month"}))
@Getter
@Setter
@NoArgsConstructor
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Long readingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "reading_month", nullable = false)
    private YearMonth readingMonth;

    @Column(name = "electricity_old", nullable = false)
    private BigDecimal electricityOld;

    @Column(name = "electricity_new", nullable = false)
    private BigDecimal electricityNew;

    @Column(name = "water_old", nullable = false)
    private BigDecimal waterOld;

    @Column(name = "water_new", nullable = false)
    private BigDecimal waterNew;

    @Column(name = "electricity_price", nullable = false)
    private BigDecimal electricityPrice;

    @Column(name = "water_price", nullable = false)
    private BigDecimal waterPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReadingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}

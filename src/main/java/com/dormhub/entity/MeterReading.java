package com.dormhub.entity;

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

@Entity
@Table(name = "meter_readings", uniqueConstraints = @UniqueConstraint(
        name = "uk_meter_readings_room_month", columnNames = {"room_id", "reading_month"}))
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

    protected MeterReading() {
    }

    public MeterReading(Room room, YearMonth readingMonth,
            BigDecimal electricityOld, BigDecimal electricityNew,
            BigDecimal waterOld, BigDecimal waterNew,
            BigDecimal electricityPrice, BigDecimal waterPrice,
            ReadingStatus status, LocalDateTime createdAt) {
        this.room = room;
        this.readingMonth = readingMonth;
        this.electricityOld = electricityOld;
        this.electricityNew = electricityNew;
        this.waterOld = waterOld;
        this.waterNew = waterNew;
        this.electricityPrice = electricityPrice;
        this.waterPrice = waterPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getReadingId() {
        return readingId;
    }

    public Room getRoom() {
        return room;
    }

    public YearMonth getReadingMonth() {
        return readingMonth;
    }

    public BigDecimal getElectricityOld() {
        return electricityOld;
    }

    public BigDecimal getElectricityNew() {
        return electricityNew;
    }

    public BigDecimal getWaterOld() {
        return waterOld;
    }

    public BigDecimal getWaterNew() {
        return waterNew;
    }

    public BigDecimal getElectricityPrice() {
        return electricityPrice;
    }

    public BigDecimal getWaterPrice() {
        return waterPrice;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

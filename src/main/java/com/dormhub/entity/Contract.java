package com.dormhub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 38, scale = 0)
    private BigDecimal monthlyRent;

    @Column(name = "deposit_amount", precision = 38, scale = 0)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ContractStatus status;

    public Contract(Room room, LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal monthlyRent, ContractStatus status) {
        this(room, startDate, endDate, monthlyRent, null, status);
    }

    public Contract(Room room, LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, ContractStatus status) {
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.depositAmount = depositAmount;
        this.status = status;
    }

}

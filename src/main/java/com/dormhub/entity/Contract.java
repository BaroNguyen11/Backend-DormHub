package com.dormhub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ContractStatus status;

    protected Contract() {}

    public Contract(Room room, LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal monthlyRent, ContractStatus status) {
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.status = status;
    }

    public Long getContractId() { return contractId; }
    public Room getRoom() { return room; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public ContractStatus getStatus() { return status; }
}

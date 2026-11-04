package com.dormhub.entity;

import com.dormhub.entity.enums.SepayTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sepay_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sepay_transactions_transaction_id", columnNames = "sepay_transaction_id"),
        indexes = {
                @Index(name = "idx_sepay_transactions_payment", columnList = "payment_id"),
                @Index(name = "idx_sepay_transactions_status_received", columnList = "status,received_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class SepayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sepay_transaction_id", nullable = false, updatable = false)
    private Long sepayTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", foreignKey = @ForeignKey(name = "fk_sepay_transactions_payment"))
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SepayTransactionStatus status = SepayTransactionStatus.UNMATCHED;

    @Column(name = "gateway", nullable = false, length = 100)
    private String gateway;

    @Column(name = "account_number", nullable = false, length = 32)
    private String accountNumber;

    @Column(name = "transfer_type", nullable = false, length = 8)
    private String transferType;

    @Column(name = "transfer_amount", nullable = false, precision = 38, scale = 0)
    private BigDecimal transferAmount;

    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "bank_reference_code", length = 255)
    private String bankReferenceCode;

    @Column(name = "transaction_at", nullable = false)
    private Instant transactionAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}

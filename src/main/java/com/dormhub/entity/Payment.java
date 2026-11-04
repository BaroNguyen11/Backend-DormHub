package com.dormhub.entity;

import com.dormhub.entity.enums.PaymentMethod;
import com.dormhub.entity.enums.PaymentStatus;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payments_payment_reference", columnNames = "payment_reference"),
        indexes = @Index(name = "idx_payments_invoice_status", columnList = "invoice_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_invoice"))
    private Invoice invoice;

    @Column(name = "payment_reference", nullable = false, updatable = false, length = 64)
    @Setter(AccessLevel.NONE)
    private String paymentReference;

    @Column(name = "expected_amount", nullable = false, updatable = false, precision = 38, scale = 0)
    @Setter(AccessLevel.NONE)
    private BigDecimal expectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, updatable = false, length = 20)
    @Setter(AccessLevel.NONE)
    private PaymentMethod method = PaymentMethod.BANK_TRANSFER;

    @Column(name = "receiving_bank_code", nullable = false, updatable = false, length = 32)
    @Setter(AccessLevel.NONE)
    private String receivingBankCode;

    @Column(name = "receiving_account_number", nullable = false, updatable = false, length = 32)
    @Setter(AccessLevel.NONE)
    private String receivingAccountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    public Payment(Invoice invoice, String paymentReference, BigDecimal expectedAmount,
                   String receivingBankCode, String receivingAccountNumber, Instant expiresAt) {
        this.invoice = invoice;
        this.paymentReference = paymentReference;
        this.expectedAmount = expectedAmount;
        this.receivingBankCode = receivingBankCode;
        this.receivingAccountNumber = receivingAccountNumber;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

package com.dormhub.entity;

import com.dormhub.config.YearMonthAttributeConverter;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(
        name = "uk_invoices_room_month", columnNames = {"room_id", "billing_month"}))
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 32)
    private InvoiceType invoiceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month", nullable = false)
    private YearMonth billingMonth;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "total_amount", nullable = false, precision = 38, scale = 0)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceDetail> details = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(Room room, YearMonth billingMonth, LocalDateTime issueDate, BigDecimal totalAmount) {
        this.invoiceType = InvoiceType.UTILITY;
        this.room = room;
        this.billingMonth = billingMonth;
        this.issueDate = issueDate;
        this.dueDate = issueDate.plusDays(14);
        this.status = InvoiceStatus.UNPAID;
        this.totalAmount = totalAmount;
    }

    public Invoice(Contract contract, YearMonth billingMonth, LocalDateTime issueDate, BigDecimal totalAmount) {
        this.invoiceType = InvoiceType.ROOM_FEE;
        this.contract = contract;
        this.billingMonth = billingMonth;
        this.issueDate = issueDate;
        this.dueDate = issueDate.plusDays(14);
        this.status = InvoiceStatus.UNPAID;
        this.totalAmount = totalAmount;
    }

    public void addDetail(InvoiceDetail detail) {
        details.add(detail);
        detail.setInvoice(this);
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public Room getRoom() {
        return room;
    }

    public Contract getContract() { return contract; }
    public LocalDateTime getDueDate() { return dueDate; }
    public InvoiceStatus getStatus() { return status; }

    public YearMonth getBillingMonth() {
        return billingMonth;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<InvoiceDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }
}

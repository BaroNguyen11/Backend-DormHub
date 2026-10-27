package com.dormhub.entity;

import jakarta.persistence.Column;
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
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_details")
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 32)
    private InvoiceItemType itemType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false, columnDefinition = "numeric")
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, columnDefinition = "numeric")
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 38, scale = 0)
    private BigDecimal amount;

    protected InvoiceDetail() {
    }

    public InvoiceDetail(InvoiceItemType itemType, String description,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.itemType = itemType;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Long getDetailId() {
        return detailId;
    }

    public InvoiceItemType getItemType() {
        return itemType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}

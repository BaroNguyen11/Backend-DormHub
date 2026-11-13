package com.dormhub.service;

import com.dormhub.dto.request.SepayWebhookPayload;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Payment;
import com.dormhub.entity.SepayTransaction;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.PaymentMethod;
import com.dormhub.entity.enums.PaymentStatus;
import com.dormhub.entity.enums.SepayTransactionStatus;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.PaymentRepository;
import com.dormhub.repository.SepayTransactionRepository;
import com.dormhub.util.PaymentBankMapping;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SepayWebhookService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SEPAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TRANSACTION_UNIQUE = "uk_sepay_transactions_transaction_id";

    private final SepayTransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final DepositService depositService;

    public SepayWebhookService(SepayTransactionRepository transactionRepository,
                               PaymentRepository paymentRepository,
                               InvoiceRepository invoiceRepository,
                               DepositService depositService) {
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.depositService = depositService;
    }

    public enum Outcome {
        MATCHED, UNMATCHED, REVIEW_REQUIRED, DUPLICATE, INVALID, CONFLICT
    }

    @Transactional
    public Outcome reconcile(SepayWebhookPayload payload) {
        Instant transactionAt = transactionTime(payload);
        if (!valid(payload, transactionAt)) {
            return Outcome.INVALID;
        }

        SepayTransaction existing = transactionRepository.findBySepayTransactionId(payload.getId()).orElse(null);
        if (existing != null) {
            return sameTransaction(existing, payload, transactionAt) ? Outcome.DUPLICATE : Outcome.CONFLICT;
        }

        SepayTransaction transaction = newTransaction(payload, transactionAt);
        if (!"in".equals(payload.getTransferType())) {
            return review(transaction, null, "Transfer is not incoming");
        }
        if (payload.getCode() == null || payload.getCode().isBlank()) {
            transactionRepository.saveAndFlush(transaction);
            return Outcome.UNMATCHED;
        }

        Payment candidate = paymentRepository.findByPaymentReference(payload.getCode()).orElse(null);
        if (candidate == null) {
            transactionRepository.saveAndFlush(transaction);
            return Outcome.UNMATCHED;
        }
        if (candidate.getInvoice() == null || candidate.getInvoice().getInvoiceId() == null) {
            return review(transaction, candidate, "Payment has no invoice");
        }

        // Keep the same lock order as PaymentService.initiate(): Invoice, then Payment.
        Invoice invoice = invoiceRepository.findByIdForUpdate(candidate.getInvoice().getInvoiceId()).orElse(null);
        Payment payment = paymentRepository.findByIdForUpdate(candidate.getPaymentId()).orElse(null);
        if (invoice == null || payment == null || payment.getInvoice() == null
                || !invoice.getInvoiceId().equals(payment.getInvoice().getInvoiceId())
                || !payload.getCode().equals(payment.getPaymentReference())) {
            return review(transaction, payment, "Payment relationship changed");
        }

        // Another delivery may have committed while the invoice lock was pending.
        existing = transactionRepository.findBySepayTransactionId(payload.getId()).orElse(null);
        if (existing != null) {
            return sameTransaction(existing, payload, transactionAt) ? Outcome.DUPLICATE : Outcome.CONFLICT;
        }
        transaction.setPayment(payment);
        if (invoice.getInvoiceType() != InvoiceType.ROOM_FEE
                && invoice.getInvoiceType() != InvoiceType.UTILITY
                && invoice.getInvoiceType() != InvoiceType.DEPOSIT) {
            return review(transaction, payment, "Invoice type is not supported");
        }
        String gatewayBin = PaymentBankMapping.sepayGatewayBin(payload.getGateway());
        String receivingBankBin = PaymentBankMapping.vietQrBankBin(payment.getReceivingBankCode());
        if (gatewayBin == null || !gatewayBin.equals(receivingBankBin)) {
            return review(transaction, payment, "Receiving bank cannot be verified");
        }
        if (!payload.getAccountNumber().equals(payment.getReceivingAccountNumber())) {
            return review(transaction, payment, "Receiving account does not match");
        }
        if (payment.getMethod() != PaymentMethod.BANK_TRANSFER
                || payment.getExpectedAmount() == null || invoice.getTotalAmount() == null
                || payload.getTransferAmount().compareTo(payment.getExpectedAmount()) != 0
                || payload.getTransferAmount().compareTo(invoice.getTotalAmount()) != 0) {
            return review(transaction, payment, "Amount or payment method does not match");
        }
        if (!canSettle(payment, invoice)) {
            return review(transaction, payment, "Payment or invoice is no longer payable");
        }
        if (payment.getCreatedAt() == null
                || transactionAt.isBefore(payment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS))) {
            return review(transaction, payment, "Transfer predates payment creation");
        }
        if (payment.getExpiresAt() == null || !transactionAt.isBefore(payment.getExpiresAt())) {
            return review(transaction, payment, "Bank transfer occurred after payment expiry");
        }

        transaction.setStatus(SepayTransactionStatus.MATCHED);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(transactionAt);
        cancelOtherPendingPayments(invoice.getInvoiceId(), payment.getPaymentId());
        if (invoice.getInvoiceType() == InvoiceType.DEPOSIT) {
            depositService.confirmDepositPaid(invoice.getInvoiceId());
        } else {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        transactionRepository.saveAndFlush(transaction);
        return Outcome.MATCHED;
    }

    @Transactional(readOnly = true)
    public Outcome resolveDuplicate(SepayWebhookPayload payload) {
        Instant transactionAt = transactionTime(payload);
        if (!valid(payload, transactionAt)) {
            return Outcome.INVALID;
        }
        return transactionRepository.findBySepayTransactionId(payload.getId())
                .map(existing -> sameTransaction(existing, payload, transactionAt) ? Outcome.DUPLICATE : Outcome.CONFLICT)
                .orElse(Outcome.CONFLICT);
    }

    @Transactional
    public Outcome retryReconcile(Long transactionId) {
        SepayTransaction transaction = transactionRepository.findByIdForUpdate(transactionId).orElse(null);
        if (transaction == null) {
            return Outcome.UNMATCHED;
        }
        if (transaction.getStatus() != SepayTransactionStatus.UNMATCHED
                || transaction.getCode() == null || transaction.getCode().isBlank()) {
            return transaction.getStatus() == SepayTransactionStatus.MATCHED ? Outcome.MATCHED
                    : transaction.getStatus() == SepayTransactionStatus.REVIEW_REQUIRED
                    ? Outcome.REVIEW_REQUIRED : Outcome.UNMATCHED;
        }
        Payment candidate = paymentRepository.findByPaymentReference(transaction.getCode()).orElse(null);
        if (candidate == null || candidate.getInvoice() == null) {
            return Outcome.UNMATCHED;
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(candidate.getInvoice().getInvoiceId()).orElse(null);
        Payment payment = paymentRepository.findByIdForUpdate(candidate.getPaymentId()).orElse(null);
        if (invoice == null || payment == null || payment.getInvoice() == null
                || !invoice.getInvoiceId().equals(payment.getInvoice().getInvoiceId())
                || !transaction.getCode().equals(payment.getPaymentReference())) {
            return Outcome.UNMATCHED;
        }

        transaction.setPayment(payment);
        if (invoice.getInvoiceType() != InvoiceType.ROOM_FEE
                && invoice.getInvoiceType() != InvoiceType.UTILITY
                && invoice.getInvoiceType() != InvoiceType.DEPOSIT) {
            return review(transaction, payment, "Invoice type is not supported");
        }
        String gatewayBin = PaymentBankMapping.sepayGatewayBin(transaction.getGateway());
        String receivingBankBin = PaymentBankMapping.vietQrBankBin(payment.getReceivingBankCode());
        if (gatewayBin == null || !gatewayBin.equals(receivingBankBin)) {
            return review(transaction, payment, "Receiving bank cannot be verified");
        }
        if (!transaction.getAccountNumber().equals(payment.getReceivingAccountNumber())) {
            return review(transaction, payment, "Receiving account does not match");
        }
        if (payment.getMethod() != PaymentMethod.BANK_TRANSFER
                || payment.getExpectedAmount() == null || invoice.getTotalAmount() == null
                || transaction.getTransferAmount().compareTo(payment.getExpectedAmount()) != 0
                || transaction.getTransferAmount().compareTo(invoice.getTotalAmount()) != 0) {
            return review(transaction, payment, "Amount or payment method does not match");
        }
        if (!canSettle(payment, invoice)) {
            return review(transaction, payment, "Payment or invoice is no longer payable");
        }
        Instant transactionAt = transaction.getTransactionAt();
        if (payment.getCreatedAt() == null
                || transactionAt.isBefore(payment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS))) {
            return review(transaction, payment, "Transfer predates payment creation");
        }
        if (payment.getExpiresAt() == null || !transactionAt.isBefore(payment.getExpiresAt())) {
            return review(transaction, payment, "Bank transfer occurred after payment expiry");
        }

        transaction.setStatus(SepayTransactionStatus.MATCHED);
        transaction.setReviewReason(null);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(transactionAt);
        cancelOtherPendingPayments(invoice.getInvoiceId(), payment.getPaymentId());
        if (invoice.getInvoiceType() == InvoiceType.DEPOSIT) {
            depositService.confirmDepositPaid(invoice.getInvoiceId());
        } else {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        transactionRepository.saveAndFlush(transaction);
        return Outcome.MATCHED;
    }

    private boolean canSettle(Payment payment, Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.UNPAID
                && (payment.getStatus() == PaymentStatus.PENDING
                    || payment.getStatus() == PaymentStatus.EXPIRED)
                && paymentRepository.findFirstByInvoice_InvoiceIdAndStatusOrderByCreatedAtDesc(
                        invoice.getInvoiceId(), PaymentStatus.SUCCESS).isEmpty();
    }

    private void cancelOtherPendingPayments(Long invoiceId, Long successfulPaymentId) {
        for (Payment other : paymentRepository.findAllByInvoice_InvoiceIdOrderByCreatedAtDesc(invoiceId)) {
            if (!other.getPaymentId().equals(successfulPaymentId)
                    && other.getStatus() == PaymentStatus.PENDING) {
                other.setStatus(PaymentStatus.CANCELLED);
            }
        }
    }

    public boolean isTransactionIdUniqueViolation(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraint
                    && TRANSACTION_UNIQUE.equals(constraint.getConstraintName())) {
                return true;
            }
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())
                    && sql.getMessage() != null && sql.getMessage().contains(TRANSACTION_UNIQUE)) {
                return true;
            }
        }
        return false;
    }

    private Outcome review(SepayTransaction transaction, Payment payment, String reason) {
        transaction.setPayment(payment);
        transaction.setStatus(SepayTransactionStatus.REVIEW_REQUIRED);
        transaction.setReviewReason(reason);
        transactionRepository.saveAndFlush(transaction);
        return Outcome.REVIEW_REQUIRED;
    }

    private SepayTransaction newTransaction(SepayWebhookPayload payload, Instant transactionAt) {
        SepayTransaction transaction = new SepayTransaction();
        transaction.setSepayTransactionId(payload.getId());
        transaction.setGateway(payload.getGateway());
        transaction.setAccountNumber(payload.getAccountNumber());
        transaction.setTransferType(payload.getTransferType());
        transaction.setTransferAmount(payload.getTransferAmount());
        transaction.setCode(payload.getCode());
        transaction.setContent(payload.getContent());
        transaction.setBankReferenceCode(payload.getReferenceCode());
        transaction.setTransactionAt(transactionAt);
        return transaction;
    }

    private Instant transactionTime(SepayWebhookPayload payload) {
        if (payload == null || payload.getTransactionDate() == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(payload.getTransactionDate(), SEPAY_DATE).atZone(VIETNAM).toInstant();
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private boolean valid(SepayWebhookPayload payload, Instant transactionAt) {
        BigDecimal amount = payload == null ? null : payload.getTransferAmount();
        return payload != null && payload.getId() != null && payload.getId() > 0
                && transactionAt != null
                && presentWithin(payload.getGateway(), 100)
                && presentWithin(payload.getAccountNumber(), 32)
                && optionalWithin(payload.getCode(), 64)
                && optionalWithin(payload.getReferenceCode(), 255)
                && ("in".equals(payload.getTransferType()) || "out".equals(payload.getTransferType()))
                && amount != null && amount.signum() > 0 && amount.stripTrailingZeros().scale() <= 0
                && amount.setScale(0, RoundingMode.UNNECESSARY).precision() <= 38;
    }

    private boolean presentWithin(String value, int maxLength) {
        return value != null && !value.isBlank() && value.length() <= maxLength;
    }

    private boolean optionalWithin(String value, int maxLength) {
        return value == null || value.length() <= maxLength;
    }

    private boolean sameTransaction(SepayTransaction stored, SepayWebhookPayload payload, Instant transactionAt) {
        return Objects.equals(stored.getGateway(), payload.getGateway())
                && Objects.equals(stored.getAccountNumber(), payload.getAccountNumber())
                && Objects.equals(stored.getTransferType(), payload.getTransferType())
                && stored.getTransferAmount().compareTo(payload.getTransferAmount()) == 0
                && Objects.equals(stored.getCode(), payload.getCode())
                && Objects.equals(stored.getContent(), payload.getContent())
                && Objects.equals(stored.getBankReferenceCode(), payload.getReferenceCode())
                && Objects.equals(stored.getTransactionAt(), transactionAt);
    }
}

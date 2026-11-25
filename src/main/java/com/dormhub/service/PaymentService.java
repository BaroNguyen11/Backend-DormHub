package com.dormhub.service;

import com.dormhub.config.PaymentQrProperties;
import com.dormhub.dto.response.StudentPaymentResponse;
import com.dormhub.dto.response.StudentPaymentHistoryResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Payment;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.PaymentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.PaymentRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.util.PaymentBankMapping;
import com.dormhub.util.VietQrUrlBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_REFERENCE_ATTEMPTS = 16;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final PaymentQrProperties properties;
    private final VietQrUrlBuilder vietQrUrlBuilder;

    public PaymentService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                          RoomAssignmentRepository roomAssignmentRepository, PaymentQrProperties properties,
                          VietQrUrlBuilder vietQrUrlBuilder) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.properties = properties;
        this.vietQrUrlBuilder = vietQrUrlBuilder;
    }

    @Transactional
    public InitiationResult initiate(Long invoiceId, User currentUser) {
        requireUser(currentUser);
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND));
        requireInvoiceAccess(invoice, currentUser);
        if (invoice.getInvoiceType() != InvoiceType.ROOM_FEE
                && invoice.getInvoiceType() != InvoiceType.UTILITY
                && invoice.getInvoiceType() != InvoiceType.DEPOSIT) {
            throw new AppException(ErrorCode.PAYMENT_NOT_SUPPORTED);
        }
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new AppException(ErrorCode.PAYMENT_CONFLICT);
        }
        Instant depositDeadline = invoice.getInvoiceType() == InvoiceType.DEPOSIT
                && invoice.getContract() != null
                && invoice.getDueDate() != null
                ? invoice.getDueDate().atZone(BUSINESS_ZONE).toInstant() : null;
        if (depositDeadline != null && !Instant.now().isBefore(depositDeadline)) {
            throw new AppException(ErrorCode.PAYMENT_CONFLICT, "Đã quá hạn thanh toán tiền cọc");
        }

        BigDecimal amount = invoice.getTotalAmount();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        properties.validateForInitiation();
        if (PaymentBankMapping.vietQrBankBin(properties.getBankCode()) == null) {
            throw new AppException(ErrorCode.PAYMENT_CONFIGURATION_ERROR);
        }

        Instant now = Instant.now();
        Payment active = null;
        for (Payment existing : paymentRepository.findAllByInvoice_InvoiceIdOrderByCreatedAtDesc(invoiceId)) {
            if (existing.getStatus() != PaymentStatus.PENDING) {
                continue;
            }
            if (!existing.getExpiresAt().isAfter(now)) {
                existing.setStatus(PaymentStatus.EXPIRED);
                // Ghi tường minh (JPA dirty checking cũng tự flush khi commit,
                // nhưng để rõ ý đồ và phụ thuộc ít vào cơ chế nền).
                paymentRepository.save(existing);
            } else if (active != null) {
                throw new AppException(ErrorCode.PAYMENT_CONFLICT);
            } else {
                active = existing;
            }
        }
        if (active != null) {
            if (active.getExpectedAmount().compareTo(amount) != 0) {
                throw new AppException(ErrorCode.PAYMENT_CONFLICT);
            }
            return new InitiationResult(toResponse(active, invoice, now), false);
        }

        String reference = generateUnusedReference();
        Instant expiresAt = now.plus(properties.getTtl());
        if (depositDeadline != null && expiresAt.isAfter(depositDeadline)) {
            expiresAt = depositDeadline;
        }
        Payment payment = new Payment(invoice, reference, amount.setScale(0, RoundingMode.UNNECESSARY),
                properties.getBankCode(), properties.getAccountNumber(), expiresAt);
        Payment saved = paymentRepository.saveAndFlush(payment);
        return new InitiationResult(toResponse(saved, invoice, now), true);
    }

    @Transactional(readOnly = true)
    public StudentPaymentResponse get(Long paymentId, User currentUser) {
        requireUser(currentUser);
        if (paymentId == null || paymentId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Invoice invoice = payment.getInvoice();
        requireInvoiceAccess(invoice, currentUser);
        if (invoice.getInvoiceType() != InvoiceType.ROOM_FEE
                && invoice.getInvoiceType() != InvoiceType.UTILITY
                && invoice.getInvoiceType() != InvoiceType.DEPOSIT) {
            throw new AppException(ErrorCode.PAYMENT_NOT_SUPPORTED);
        }
        return toResponse(payment, invoice, Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<StudentPaymentHistoryResponse> history(User currentUser, PaymentStatus status, String search,
                                                        int page, int size) {
        requireUser(currentUser);
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return paymentRepository.findHistoryForStudent(currentUser.getId(),
                        status == null ? "" : status.name(), search == null ? "" : search.trim(),
                        PageRequest.of(page, size))
                .map(payment -> new StudentPaymentHistoryResponse(
                        payment.getPaymentId(), payment.getInvoice().getInvoiceId(),
                        payment.getInvoice().getInvoiceType(), payment.getPaymentReference(),
                        payment.getExpectedAmount(), payment.getStatus(),
                        payment.getInvoice().getStatus(), payment.getCreatedAt(),
                        payment.getExpiresAt(), payment.getPaidAt(),
                        payment.getStatus() == PaymentStatus.EXPIRED
                                || (payment.getStatus() == PaymentStatus.PENDING
                                    && !payment.getExpiresAt().isAfter(Instant.now()))));
    }

    private void requireUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void requireInvoiceAccess(Invoice invoice, User currentUser) {
        if (invoice.getInvoiceType() == InvoiceType.ROOM_FEE
                || invoice.getInvoiceType() == InvoiceType.DEPOSIT) {
            Contract contract = invoice.getContract();
            if (contract == null || contract.getStudent() == null
                    || contract.getStudent().getUser() == null
                    || !currentUser.getId().equals(contract.getStudent().getUser().getId())) {
                throw new AppException(ErrorCode.BILLING_NOT_FOUND);
            }
            return;
        }
        if (invoice.getInvoiceType() == InvoiceType.UTILITY) {
            if (invoice.getRoom() == null || invoice.getBillingMonth() == null) {
                throw new AppException(ErrorCode.BILLING_NOT_FOUND);
            }
            YearMonth month = invoice.getBillingMonth();
            LocalDateTime monthStart = month.atDay(1).atStartOfDay();
            LocalDateTime monthEndExclusive = month.plusMonths(1).atDay(1).atStartOfDay();
            if (!roomAssignmentRepository.hasActualOccupancyOverlappingMonth(
                    invoice.getRoom().getId(), currentUser.getId(), monthStart, monthEndExclusive)) {
                throw new AppException(ErrorCode.BILLING_NOT_FOUND);
            }
            return;
        }
        throw new AppException(ErrorCode.BILLING_NOT_FOUND);
    }

    private String generateUnusedReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            StringBuilder reference = new StringBuilder(properties.getReferencePrefix());
            for (int i = 0; i < properties.getReferenceSuffixLength(); i++) {
                reference.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            String candidate = reference.toString();
            if (paymentRepository.findByPaymentReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new AppException(ErrorCode.PAYMENT_CONFLICT);
    }

    private StudentPaymentResponse toResponse(Payment payment, Invoice invoice, Instant now) {
        boolean expired = payment.getStatus() == PaymentStatus.EXPIRED
                || (payment.getStatus() == PaymentStatus.PENDING && !payment.getExpiresAt().isAfter(now));
        String qrUrl = payment.getStatus() == PaymentStatus.PENDING
                && invoice.getStatus() == InvoiceStatus.UNPAID && !expired
                ? vietQrUrlBuilder.build(payment) : null;
        return new StudentPaymentResponse(payment.getPaymentId(), invoice.getInvoiceId(),
                invoice.getInvoiceType(), payment.getStatus(), invoice.getStatus(),
                payment.getExpectedAmount(), "VND", payment.getPaymentReference(),
                payment.getReceivingBankCode(), payment.getReceivingAccountNumber(), qrUrl,
                payment.getCreatedAt(), payment.getExpiresAt(), payment.getPaidAt(), expired);
    }

    public static class InitiationResult {

        private final StudentPaymentResponse payment;
        private final boolean created;

        public InitiationResult(StudentPaymentResponse payment, boolean created) {
            this.payment = payment;
            this.created = created;
        }

        public StudentPaymentResponse getPayment() {
            return payment;
        }

        public boolean isCreated() {
            return created;
        }
    }
}

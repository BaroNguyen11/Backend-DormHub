package com.dormhub.service;

import com.dormhub.dto.response.SepayTransactionResponse;
import com.dormhub.entity.SepayTransaction;
import com.dormhub.entity.enums.SepayTransactionStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.SepayTransactionRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra cứu + đối soát lại giao dịch SePay cho quản lý.
 *
 * <p>Quan trọng: webhook SePay trả về HTTP 200 cho cả MATCHED / UNMATCHED /
 * REVIEW_REQUIRED / DUPLICATE (chỉ INVALID/CONFLICT mới lỗi), nên nhìn ở dashboard
 * SePay "webhook thành công" KHÔNG có nghĩa là hóa đơn đã PAID. Trạng thái thật
 * nằm trong bảng sepay_transactions — màn này hiển thị đúng trạng thái + lý do
 * review để chẩn đoán "đã chuyển khoản nhưng chưa được xác nhận".
 */
@Service
public class SepayTransactionAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SepayTransactionRepository transactionRepository;
    private final SepayWebhookService webhookService;

    public SepayTransactionAdminService(SepayTransactionRepository transactionRepository,
                                        SepayWebhookService webhookService) {
        this.transactionRepository = transactionRepository;
        this.webhookService = webhookService;
    }

    @Transactional(readOnly = true)
    public Page<SepayTransactionResponse> search(SepayTransactionStatus status, int page, int size, String search) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "receivedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<SepayTransaction> transactions = transactionRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<SepayTransaction>text(search,
                        "id", "sepayTransactionId", "code", "content", "gateway", "bankReferenceCode",
                        "payment.paymentReference", "payment.invoice.invoiceId")
                .and(com.dormhub.specification.SearchSpecifications.equal("status", status)), pageable);
        return transactions.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SepayTransactionResponse get(Long transactionId) {
        return toResponse(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST,
                        "Không tìm thấy giao dịch SePay #" + transactionId)));
    }

    /**
     * Đối soát lại một giao dịch UNMATCHED — hữu dụng khi sinh viên chuyển khoản
     * TRƯỚC khi bấm "Thanh toán" (lúc webhook đến chưa có phiên thanh toán nào
     * mang mã tham chiếu đó). Sau khi tạo phiên, bấm nút này để hệ thống ghép lại.
     */
    @Transactional
    public SepayTransactionResponse retry(Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "transactionId phải là số dương");
        }
        SepayTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST,
                        "Không tìm thấy giao dịch SePay #" + transactionId));
        if (transaction.getStatus() != SepayTransactionStatus.UNMATCHED) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể đối soát lại giao dịch ở trạng thái UNMATCHED (hiện tại: "
                            + transaction.getStatus() + ")");
        }
        webhookService.retryReconcile(transactionId);
        return toResponse(transaction);
    }

    @Transactional
    public SepayTransactionResponse recordReview(Long transactionId, Long managerUserId, String note) {
        if (transactionId == null || transactionId <= 0 || managerUserId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        SepayTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST,
                        "Không tìm thấy giao dịch SePay #" + transactionId));
        if (transaction.getStatus() != SepayTransactionStatus.REVIEW_REQUIRED) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chỉ ghi chú rà soát cho giao dịch REVIEW_REQUIRED");
        }
        if (note == null || note.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần nhập ghi chú rà soát");
        }
        String entry = " | Manager #" + managerUserId + " @ " + Instant.now() + ": " + note.trim();
        String updatedReason = (transaction.getReviewReason() == null
                ? "Cần rà soát" : transaction.getReviewReason()) + entry;
        if (updatedReason.length() > 500) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Ghi chú rà soát vượt giới hạn lưu trữ; cần xử lý trực tiếp với bộ phận tài chính");
        }
        transaction.setReviewReason(updatedReason);
        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    private SepayTransactionResponse toResponse(SepayTransaction transaction) {
        Long paymentId = transaction.getPayment() == null ? null : transaction.getPayment().getPaymentId();
        Long invoiceId = transaction.getPayment() == null || transaction.getPayment().getInvoice() == null
                ? null
                : transaction.getPayment().getInvoice().getInvoiceId();
        String paymentReference = transaction.getPayment() == null
                ? null
                : transaction.getPayment().getPaymentReference();
        return new SepayTransactionResponse(
                transaction.getId(),
                transaction.getSepayTransactionId(),
                paymentId,
                invoiceId,
                paymentReference,
                transaction.getStatus(),
                transaction.getGateway(),
                transaction.getAccountNumber(),
                transaction.getTransferType(),
                transaction.getTransferAmount(),
                transaction.getCode(),
                transaction.getContent(),
                transaction.getBankReferenceCode(),
                transaction.getTransactionAt(),
                transaction.getReceivedAt(),
                transaction.getReviewReason());
    }
}

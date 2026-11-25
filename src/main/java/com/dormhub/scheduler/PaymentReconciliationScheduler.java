package com.dormhub.scheduler;

import com.dormhub.entity.SepayTransaction;
import com.dormhub.entity.enums.SepayTransactionStatus;
import com.dormhub.repository.SepayTransactionRepository;
import com.dormhub.service.SepayWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);
    private final SepayTransactionRepository transactionRepository;
    private final SepayWebhookService webhookService;

    public PaymentReconciliationScheduler(SepayTransactionRepository transactionRepository,
                                          SepayWebhookService webhookService) {
        this.transactionRepository = transactionRepository;
        this.webhookService = webhookService;
    }

    // Chạy mỗi 10 phút để đối soát bù các giao dịch UNMATCHED trong vòng 24 giờ qua
    @Scheduled(cron = "${dormhub.payment.reconcile-cron:0 */10 * * * *}")
    public void retryUnmatchedTransactions() {
        log.info("Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions START at={}", Instant.now());
        try {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<SepayTransaction> unmatched = transactionRepository
                .findByStatusAndReceivedAtAfter(SepayTransactionStatus.UNMATCHED, since);

        if (unmatched.isEmpty()) {
            log.info("Payment reconciliation candidates=0, matched=0");
            return;
        }

        log.info("Bắt đầu đối soát lại {} giao dịch UNMATCHED", unmatched.size());
        int matchedCount = 0;
        for (SepayTransaction tx : unmatched) {
            try {
                SepayWebhookService.Outcome outcome = webhookService.retryReconcile(tx.getId());
                if (outcome == SepayWebhookService.Outcome.MATCHED) {
                    matchedCount++;
                    log.info("Đối soát bù thành công giao dịch SePay #{}", tx.getSepayTransactionId());
                }
            } catch (Exception e) {
                log.warn("Lỗi khi đối soát lại giao dịch #{}: {}", tx.getSepayTransactionId(), e.getMessage());
            }
        }
        log.info("Payment reconciliation candidates={}, matched={}", unmatched.size(), matchedCount);
        } finally {
            log.info("Scheduler PaymentReconciliationScheduler.retryUnmatchedTransactions END at={}", Instant.now());
        }
    }
}


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
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<SepayTransaction> unmatched = transactionRepository
                .findByStatusAndReceivedAtAfter(SepayTransactionStatus.UNMATCHED, since);

        if (unmatched.isEmpty()) {
            return;
        }

        log.info("Bắt đầu đối soát lại {} giao dịch UNMATCHED", unmatched.size());
        int matchedCount = 0;
        for (SepayTransaction tx : unmatched) {
            try {
                SepayWebhookService.Outcome outcome = webhookService.retryReconcile(tx);
                if (outcome == SepayWebhookService.Outcome.MATCHED) {
                    matchedCount++;
                    log.info("Đối soát bù thành công giao dịch SePay #{}", tx.getSepayTransactionId());
                }
            } catch (Exception e) {
                log.warn("Lỗi khi đối soát lại giao dịch #{}: {}", tx.getSepayTransactionId(), e.getMessage());
            }
        }
        if (matchedCount > 0) {
            log.info("Đối soát bù hoàn tất: khớp thành công {}/{} giao dịch", matchedCount, unmatched.size());
        }
    }
}


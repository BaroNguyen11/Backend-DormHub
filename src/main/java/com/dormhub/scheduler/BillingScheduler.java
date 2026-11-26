package com.dormhub.scheduler;

import com.dormhub.repository.DistributedLockRepository;
import com.dormhub.service.BillingBatchService;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long BILLING_LOCK_KEY = 824001L;
    private static final long UTILITY_LOCK_KEY = 824002L;

    private final BillingBatchService billingBatchService;
    private final TaskScheduler taskScheduler;
    private final DistributedLockRepository distributedLockRepository;
    private final AtomicBoolean applicationReady = new AtomicBoolean();
    @Value("${billing.room-fee.reconcile-on-startup:true}")
    private boolean reconcileOnStartup;
    @Value("${billing.utility.reconcile-on-startup:true}")
    private boolean utilityReconcileOnStartup;
    private final AtomicBoolean utilityStartupScheduled = new AtomicBoolean();

    public BillingScheduler(BillingBatchService billingBatchService,
                            TaskScheduler taskScheduler,
                            DistributedLockRepository distributedLockRepository) {
        this.billingBatchService = billingBatchService;
        this.taskScheduler = taskScheduler;
        this.distributedLockRepository = distributedLockRepository;
    }

    @EventListener
    public void reconcileCurrentMonth(AvailabilityChangeEvent<ReadinessState> event) {
        if (event.getState() == ReadinessState.ACCEPTING_TRAFFIC) {
            applicationReady.set(true);
            if (reconcileOnStartup) {
                taskScheduler.schedule(() -> runCurrentMonth("reconcileCurrentMonth"), Instant.now());
            } else {
                log.info("Scheduler BillingScheduler.reconcileCurrentMonth disabled on startup");
            }
            if (utilityReconcileOnStartup && utilityStartupScheduled.compareAndSet(false, true)) {
                log.info("Scheduler BillingScheduler.generateUtilityInvoices scheduled on startup");
                taskScheduler.schedule(this::generateUtilityInvoices, Instant.now());
            } else if (!utilityReconcileOnStartup) {
                log.info("Scheduler BillingScheduler.generateUtilityInvoices disabled on startup");
            }
        }
    }

    @Scheduled(cron = "${billing.room-fee.cron:0 0 1 1 * *}", zone = "Asia/Ho_Chi_Minh")
    public void generateMonthlyRoomFees() {
        if (applicationReady.get()) {
            runCurrentMonth("generateMonthlyRoomFees");
        }
    }

    @Scheduled(cron = "${billing.room-fee.reconcile-cron:0 0 2 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void reconcileDailyRoomFees() {
        if (applicationReady.get()) {
            runCurrentMonth("reconcileDailyRoomFees");
        }
    }

    @Scheduled(cron = "${billing.utility.cron:0 0 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void generateUtilityInvoices() {
        if (!applicationReady.get()) return;
        log.info("Scheduler BillingScheduler.generateUtilityInvoices START at={}", Instant.now());
        try {
            if (!distributedLockRepository.runWithLock(UTILITY_LOCK_KEY, billingBatchService::generateUtilityInvoices)) {
                log.info("UTILITY billing skipped: another instance holds the lock");
            }
        } catch (Exception error) {
            log.error("UTILITY scheduler failed: errorType={}", error.getClass().getSimpleName());
            throw error;
        } finally {
            log.info("Scheduler BillingScheduler.generateUtilityInvoices END at={}", Instant.now());
        }
    }

    private void runCurrentMonth(String trigger) {
        log.info("Scheduler BillingScheduler.{} START at={}", trigger, Instant.now());
        try {
        // Lock giữ trên đúng 1 connection suốt tác vụ, tự nhả an toàn cả khi lỗi lẫn khi JVM chết
        boolean executed = distributedLockRepository.runWithLock(BILLING_LOCK_KEY, () -> {
            YearMonth current = YearMonth.now(BUSINESS_ZONE);
            YearMonth earliest = billingBatchService.earliestActiveMonth();
            if (earliest != null) {
                for (YearMonth month = earliest; month.isBefore(current); month = month.plusMonths(1)) {
                    billingBatchService.generateRoomFees(month);
                }
            }
            billingBatchService.generateRoomFees(current);
        });
        if (!executed) {
            log.info("Bỏ qua tiến trình lập hóa đơn tháng: một instance khác đang thực thi.");
        }
        } finally {
            log.info("Scheduler BillingScheduler.{} END at={}", trigger, Instant.now());
        }
    }
}

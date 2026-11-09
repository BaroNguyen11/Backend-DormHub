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

@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long BILLING_LOCK_KEY = 824001L;

    private final BillingBatchService billingBatchService;
    private final TaskScheduler taskScheduler;
    private final DistributedLockRepository distributedLockRepository;
    private final AtomicBoolean applicationReady = new AtomicBoolean();

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
            taskScheduler.schedule(this::runCurrentMonth, Instant.now());
        }
    }

    @Scheduled(cron = "${billing.room-fee.cron:0 0 1 1 * *}", zone = "Asia/Ho_Chi_Minh")
    public void generateMonthlyRoomFees() {
        if (applicationReady.get()) {
            runCurrentMonth();
        }
    }

    @Scheduled(cron = "${billing.room-fee.reconcile-cron:0 0 2 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void reconcileDailyRoomFees() {
        if (applicationReady.get()) {
            runCurrentMonth();
        }
    }

    private void runCurrentMonth() {
        // Lock giữ trên đúng 1 connection suốt tác vụ, tự nhả an toàn cả khi lỗi lẫn khi JVM chết
        boolean executed = distributedLockRepository.runWithLock(BILLING_LOCK_KEY, () ->
                billingBatchService.generateRoomFees(YearMonth.now(BUSINESS_ZONE)));
        if (!executed) {
            log.info("Bỏ qua tiến trình lập hóa đơn tháng: một instance khác đang thực thi.");
        }
    }
}

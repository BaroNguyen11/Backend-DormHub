package com.dormhub.scheduler;

import com.dormhub.service.BillingBatchService;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class BillingScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BillingBatchService billingBatchService;
    private final TaskScheduler taskScheduler;
    private final AtomicBoolean applicationReady = new AtomicBoolean();
    private final ReentrantLock executionLock = new ReentrantLock();

    public BillingScheduler(BillingBatchService billingBatchService, TaskScheduler taskScheduler) {
        this.billingBatchService = billingBatchService;
        this.taskScheduler = taskScheduler;
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
        executionLock.lock();
        try {
            billingBatchService.generateRoomFees(YearMonth.now(BUSINESS_ZONE));
        } finally {
            executionLock.unlock();
        }
    }
}

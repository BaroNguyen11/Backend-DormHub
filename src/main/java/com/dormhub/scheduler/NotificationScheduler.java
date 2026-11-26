package com.dormhub.scheduler;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.NotificationReceiverRepository;
import com.dormhub.repository.NotificationRepository;
import com.dormhub.service.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job định kỳ cho hóa đơn & thông báo (múi giờ nghiệp vụ Asia/Ho_Chi_Minh):
 * - 09:00: nhắc hóa đơn sắp đến hạn trong 3 ngày (t-3 ngày) — gửi đúng 1 lần/hóa đơn.
 * - 09:30: nhắc hóa đơn đã quá hạn (OVERDUE) — gửi đúng 1 lần/hóa đơn.
 * - 03:15: dọn thông báo cũ hơn 90 ngày — XÓA NotificationReceiver (con) TRƯỚC
 *   Notification (cha) để không vi phạm khóa ngoại (trước đây job lỗi mỗi đêm).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final NotificationReceiverRepository notificationReceiverRepository;

    // Nhắc hóa đơn sắp đến hạn - chạy 09:00 giờ Việt Nam
    // (không mở readOnly tx ở đây để sendNotification tự mở tx ghi riêng cho từng thông báo)
    @Scheduled(cron = "${dormhub.scheduler.notification.due-reminder-cron:0 0 9 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void sendInvoiceDueReminders() {
        log.info("Scheduler NotificationScheduler.sendInvoiceDueReminders START at={}", java.time.Instant.now());
        try {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate threeDaysLater = today.plusDays(3);

        // Hóa đơn chưa thanh toán đến hạn trong 3 ngày tới
        List<Invoice> dueSoon = invoiceRepository.findByStatusAndDueDateBetween(
                InvoiceStatus.UNPAID,
                today.atStartOfDay(BUSINESS_ZONE).toLocalDateTime(),
                threeDaysLater.atTime(23, 59, 59));

        int sent = 0;
        for (Invoice invoice : dueSoon) {
            if (invoice.getContract() == null
                    || invoice.getContract().getStudent() == null
                    || invoice.getContract().getStudent().getUser() == null) {
                continue;
            }
            // Dedup: mỗi hóa đơn chỉ nhận 1 lần nhắc đến hạn
            if (notificationRepository.existsByTypeAndRefId(
                    NotificationType.INVOICE_DUE_REMINDER, invoice.getInvoiceId())) {
                continue;
            }
            notificationService.sendNotification(
                    NotificationType.INVOICE_DUE_REMINDER,
                    "Nhắc nhở: Hóa đơn sắp đến hạn",
                    String.format("Hóa đơn #%d sẽ đến hạn vào %s. Vui lòng thanh toán kịp thời.",
                            invoice.getInvoiceId(),
                            invoice.getDueDate().format(DATE_FORMAT)),
                    invoice.getInvoiceId(),
                    invoice.getContract().getStudent().getUser().getId());
            sent++;
        }
        log.info("Nhắc đến hạn: {} hóa đơn trong cửa sổ t-3 ngày, đã gửi {} thông báo", dueSoon.size(), sent);
        } finally {
            log.info("Scheduler NotificationScheduler.sendInvoiceDueReminders END at={}", java.time.Instant.now());
        }
    }

    // Nhắc hóa đơn quá hạn - chạy 09:30 giờ Việt Nam
    @Scheduled(cron = "${dormhub.scheduler.notification.overdue-reminder-cron:0 30 9 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void sendOverdueInvoiceReminders() {
        log.info("Scheduler NotificationScheduler.sendOverdueInvoiceReminders START at={}", java.time.Instant.now());
        try {
        // Nhất quán với định nghĩa OVERDUE: dueDate < hôm nay (theo múi giờ nghiệp vụ)
        LocalDateTime end = LocalDate.now(BUSINESS_ZONE).minusDays(1).atTime(23, 59, 59);
        LocalDateTime start = end.minusYears(2);
        List<Invoice> overdue = invoiceRepository.findByStatusAndDueDateBetween(
                InvoiceStatus.UNPAID, start, end);

        int sent = 0;
        for (Invoice invoice : overdue) {
            if (invoice.getContract() == null
                    || invoice.getContract().getStudent() == null
                    || invoice.getContract().getStudent().getUser() == null
                    || invoice.getDueDate() == null) {
                continue;
            }
            if (notificationRepository.existsByTypeAndRefId(
                    NotificationType.INVOICE_OVERDUE, invoice.getInvoiceId())) {
                continue;
            }
            notificationService.sendNotification(
                    NotificationType.INVOICE_OVERDUE,
                    "Cảnh báo: Hóa đơn đã quá hạn",
                    String.format("Hóa đơn #%d đã quá hạn thanh toán từ %s. Vui lòng thanh toán ngay để tránh gián đoạn lưu trú.",
                            invoice.getInvoiceId(),
                            invoice.getDueDate().format(DATE_FORMAT)),
                    invoice.getInvoiceId(),
                    invoice.getContract().getStudent().getUser().getId());
            sent++;
        }
        log.info("Nhắc quá hạn: {} hóa đơn OVERDUE, đã gửi {} thông báo", overdue.size(), sent);
        } finally {
            log.info("Scheduler NotificationScheduler.sendOverdueInvoiceReminders END at={}", java.time.Instant.now());
        }
    }

    // Dọn thông báo cũ hơn 90 ngày - chạy 03:15 (tránh trùng reconcile billing 02:00)
    @Scheduled(cron = "${dormhub.scheduler.notification.cleanup-cron:0 15 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Scheduler NotificationScheduler.cleanupOldNotifications START at={}", java.time.Instant.now());
        try {
        LocalDateTime cutoff = LocalDateTime.now(BUSINESS_ZONE).minusDays(90);
        // Phải xóa bản ghi con (receiver) trước bản ghi cha (notification) để không vi phạm FK
        int receivers = notificationReceiverRepository.deleteByNotification_CreatedAtBefore(cutoff);
        int notifications = notificationRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Dọn thông báo quá 90 ngày: xóa {} receiver, {} notification", receivers, notifications);
        } finally {
            log.info("Scheduler NotificationScheduler.cleanupOldNotifications END at={}", java.time.Instant.now());
        }
    }
}

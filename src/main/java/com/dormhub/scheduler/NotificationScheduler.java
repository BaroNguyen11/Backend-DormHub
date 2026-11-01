package com.dormhub.scheduler;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.NotificationRepository;
import com.dormhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {
    
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    
    // Send invoice due reminders - runs daily at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendInvoiceDueReminders() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);
        
        // Find unpaid invoices due in 3 days
        List<Invoice> dueSoon = invoiceRepository.findByStatusAndDueDateBetween(
            InvoiceStatus.UNPAID, 
            today.atStartOfDay(), 
            threeDaysLater.atTime(23, 59, 59)
        );
        
        for (Invoice invoice : dueSoon) {
            if (invoice.getContract() != null 
                && invoice.getContract().getStudent() != null 
                && invoice.getContract().getStudent().getUser() != null) {
                notificationService.sendNotification(
                    NotificationType.INVOICE_DUE_REMINDER,
                    "Nhắc nhở: Hóa đơn sắp đến hạn",
                    String.format("Hóa đơn #%d sẽ đến hạn vào %s. Vui lòng thanh toán kịp thời.",
                        invoice.getInvoiceId(),
                        invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                    invoice.getInvoiceId(),
                    invoice.getContract().getStudent().getUser().getId()
                );
            }
        }
        
        log.info("Sent {} invoice due reminders", dueSoon.size());
    }
    
    // Cleanup old notifications - runs daily at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = notificationRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted {} notifications older than 90 days", deleted);
    }
}

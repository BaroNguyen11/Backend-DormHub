package com.dormhub.scheduler;

import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.NotificationRepository;
import com.dormhub.service.NotificationService;
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
 * Vòng đời hợp đồng (plan Issue #14 + BR §17.2):
 * - 01:00 hằng ngày: hợp đồng ACTIVE đã quá endDate tự chuyển EXPIRED
 *   (trước đây hợp đồng ACTIVE vĩnh viễn, chiếm chỗ, gia hạn vô hạn).
 * - 09:00 hằng ngày: nhắc sinh viên hợp đồng sắp hết hạn trong 7 ngày tới
 *   (t-7 ngày, gửi đúng 1 lần nhờ kiểm tra thông báo đã tồn tại).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractLifecycleScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContractRepository contractRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void expireOverdueContracts() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        int expired = contractRepository.bulkExpireOverdueContracts(
                ContractStatus.ACTIVE, ContractStatus.EXPIRED, now);
        if (expired > 0) {
            log.info("Đã tự động hết hạn {} hợp đồng ACTIVE quá endDate", expired);
        }
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void remindExpiringContracts() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime sevenDaysLater = now.plusDays(7);
        List<Contract> expiring = contractRepository.findAllByStatusAndEndDateBetween(
                ContractStatus.ACTIVE, now, sevenDaysLater);

        int sent = 0;
        for (Contract contract : expiring) {
            if (contract.getEndDate() == null || contract.getStudent() == null
                    || contract.getStudent().getUser() == null) {
                continue;
            }
            // Dedup: mỗi hợp đồng chỉ nhận 1 thông báo nhắc hết hạn (type + refId)
            if (notificationRepository.existsByTypeAndRefId(
                    NotificationType.CONTRACT_EXPIRING, contract.getContractId())) {
                continue;
            }
            notificationService.sendNotification(
                    NotificationType.CONTRACT_EXPIRING,
                    "Hợp đồng sắp hết hạn",
                    String.format("Hợp đồng #%d của bạn sẽ hết hạn vào %s. Nếu muốn tiếp tục lưu trú, hãy gửi yêu cầu gia hạn.",
                            contract.getContractId(), contract.getEndDate().format(DATE_FORMAT)),
                    contract.getContractId(),
                    contract.getStudent().getUser().getId());
            sent++;
        }
        if (sent > 0) {
            log.info("Đã gửi {} thông báo nhắc hợp đồng sắp hết hạn (t-7 ngày)", sent);
        }
    }
}

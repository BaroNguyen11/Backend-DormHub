package com.dormhub.service;

import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.repository.RoomFeeBillingContractRepository;
import com.dormhub.repository.MeterReadingRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BillingBatchService {

    private static final int PAGE_SIZE = 200;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RoomFeeBillingContractRepository contractRepository;
    private final InvoiceService invoiceService;
    private final MeterReadingRepository meterReadingRepository;

    public BillingBatchService(RoomFeeBillingContractRepository contractRepository, InvoiceService invoiceService,
            MeterReadingRepository meterReadingRepository) {
        this.contractRepository = contractRepository;
        this.invoiceService = invoiceService;
        this.meterReadingRepository = meterReadingRepository;
    }

    public void generateUtilityInvoices() {
        long created = 0, skipped = 0, failed = 0, afterId = 0;
        try {
            while (true) {
                var readings = meterReadingRepository.findUnbilledConfirmedAfter(afterId,
                        YearMonth.now(BUSINESS_ZONE), PageRequest.of(0, PAGE_SIZE));
                if (readings.isEmpty()) break;
                for (var reading : readings) {
                    afterId = reading.getReadingId();
                    try {
                        if (invoiceService.createUtilityIfAbsent(reading.getRoom().getId(), reading.getReadingMonth())) created++;
                        else skipped++;
                    } catch (Exception error) {
                        failed++;
                        log.error("UTILITY billing failed: readingId={}, roomId={}, month={}, errorType={}",
                                reading.getReadingId(), reading.getRoom().getId(), reading.getReadingMonth(), error.getClass().getSimpleName());
                    }
                }
            }
        } finally {
            log.info("UTILITY billing complete: created={}, skipped={}, failed={}", created, skipped, failed);
        }
    }

    public YearMonth earliestActiveMonth() {
        LocalDateTime earliest = contractRepository.findEarliestActiveStart();
        return earliest == null ? null : YearMonth.from(earliest);
    }

    public void generateRoomFees(YearMonth month) {
        long created = 0;
        long skipped = 0;
        long failed = 0;
        long afterId = 0;
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = month.plusMonths(1).atDay(1).atStartOfDay();

        try {
            Long highestId = contractRepository.findHighestContractId();
            if (highestId == null) {
                return;
            }
            while (afterId < highestId) {
                List<Long> contractIds = contractRepository.findEligibleIdsAfter(
                        afterId, highestId, ContractStatus.ACTIVE, monthStart, nextMonthStart,
                        LocalDateTime.now(BUSINESS_ZONE),
                        PageRequest.of(0, PAGE_SIZE));
                if (contractIds.isEmpty()) {
                    break;
                }
                for (Long contractId : contractIds) {
                    afterId = contractId;
                    try {
                        if (invoiceService.createRoomFeeIfAbsent(contractId, month)) {
                            created++;
                        } else {
                            skipped++;
                        }
                    } catch (Exception exception) {
                        failed++;
                        log.error("ROOM_FEE billing failed: month={}, contractId={}", month, contractId, exception);
                    }
                }
            }
        } catch (Exception exception) {
            failed++;
            log.error("ROOM_FEE billing enumeration failed: month={}, afterContractId={}",
                    month, afterId, exception);
        } finally {
            log.info("ROOM_FEE billing complete: month={}, created={}, skipped={}, failed={}",
                    month, created, skipped, failed);
        }
    }
}

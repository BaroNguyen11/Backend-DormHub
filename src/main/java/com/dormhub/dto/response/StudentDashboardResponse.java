package com.dormhub.dto.response;

import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentDashboardResponse {

    private final String displayName;
    private final RoomInfo currentRoom;
    private final ContractInfo currentContract;
    private final List<UnpaidInvoice> unpaidInvoices;
    private final UtilityUsage utilities;

    @Getter
    @AllArgsConstructor
    public static class RoomInfo {
        private final Long roomId;
        private final String roomNumber;
        private final String buildingName;
        private final Integer floor;
        private final Integer currentOccupancy;
        private final Integer capacity;
    }

    @Getter
    @AllArgsConstructor
    public static class ContractInfo {
        private final Long contractId;
        private final ContractStatus status;
    }

    @Getter
    @AllArgsConstructor
    public static class UnpaidInvoice {
        private final Long invoiceId;
        private final InvoiceType invoiceType;
        private final YearMonth billingMonth;
        private final BigDecimal totalAmount;
        private final LocalDateTime dueDate;
        private final InvoiceStatus status;
        private final boolean overdue;
    }

    @Getter
    @AllArgsConstructor
    public static class UtilityUsage {
        private final YearMonth billingMonth;
        private final BigDecimal electricityConsumption;
        private final BigDecimal waterConsumption;
        private final BigDecimal electricityChange;
        private final BigDecimal waterChange;
    }
}

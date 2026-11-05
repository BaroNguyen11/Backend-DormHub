package com.dormhub.service;

import com.dormhub.dto.response.ManagerDashboardResponse;
import com.dormhub.dto.response.MonthlyOccupancyResponse;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.RenewalStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.TransferStatus;
import com.dormhub.repository.ManagerDashboardRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ManagerDashboardService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ManagerDashboardRepository repository;

    public ManagerDashboardService(ManagerDashboardRepository repository) {
        this.repository = repository;
    }

    public ManagerDashboardResponse getDashboard(int year) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        long activeResidents = repository.countActiveResidents(
                RoomAssignmentStatus.CHECKED_IN, ContractStatus.ACTIVE, now);
        long pendingApplications = repository.countApplications(DormApplicationStatus.PENDING);
        long pendingRequests = pendingApplications
                + repository.countRenewalRequests(RenewalStatus.PENDING)
                + repository.countTransferRequests(TransferStatus.PENDING)
                + repository.countCheckoutRequests(CheckoutRequestStatus.PENDING)
                + repository.countMaintenanceRequests(MaintenanceStatus.PENDING);
        long capacity = repository.sumCurrentCapacity();

        return new ManagerDashboardResponse(
                repository.count(), activeResidents, pendingApplications,
                repository.countInvoices(InvoiceStatus.UNPAID), pendingRequests,
                repository.countUrgentMaintenanceRequests(MaintenancePriority.URGENT,
                        List.of(MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS)),
                repository.countCompletedMaintenance(MaintenanceStatus.COMPLETED, today, tomorrow),
                repository.countCheckIns(today, tomorrow),
                monthlyOccupancy(year, YearMonth.from(now), activeResidents, capacity));
    }

    private List<MonthlyOccupancyResponse> monthlyOccupancy(int year, YearMonth currentMonth,
                                                             long activeResidents, long capacity) {
        List<MonthlyOccupancyResponse> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth period = YearMonth.of(year, month);
            if (period.isAfter(currentMonth)) {
                break;
            }
            if (period.equals(currentMonth)) {
                BigDecimal rate = capacity == 0 ? null : BigDecimal.valueOf(activeResidents)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
                months.add(new MonthlyOccupancyResponse(month, activeResidents, capacity, rate));
            } else {
                // Room.capacity has no history, so past occupancy cannot be reconstructed accurately.
                months.add(new MonthlyOccupancyResponse(month, null, null, null));
            }
        }
        return months;
    }
}

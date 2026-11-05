package com.dormhub.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ManagerDashboardResponse {

    private final long totalRooms;
    private final long activeResidents;
    private final long pendingApplications;
    private final long unpaidInvoices;
    private final long pendingRequests;
    private final long urgentMaintenanceRequests;
    private final long completedToday;
    private final long checkInsToday;
    private final List<MonthlyOccupancyResponse> monthlyOccupancy;
}

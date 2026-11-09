package com.dormhub.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSummaryResponse {
    
    private final Long totalStudents;      // count active students
    private final Long totalRooms;         // count all rooms
    private final Long occupiedRooms;      // count rooms with OCCUPIED status
    private final Long pendingApplications; // count PENDING dorm applications
    private final Long openMaintenance;    // count PENDING + IN_PROGRESS maintenance
    private final Long unpaidInvoices;     // count UNPAID invoices
}

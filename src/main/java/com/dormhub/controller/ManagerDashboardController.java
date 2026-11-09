package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.DashboardSummaryResponse;
import com.dormhub.entity.enums.*;
import com.dormhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/manager/dashboard")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final DormApplicationRepository dormApplicationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        
        // Count active students
        long totalStudents = studentRepository.count();
        
        // Count total rooms
        long totalRooms = roomRepository.count();
        
        // Count occupied rooms
        long occupiedRooms = roomRepository.countByStatus(RoomStatus.OCCUPIED);
        
        // Count pending dorm applications
        long pendingApplications = dormApplicationRepository.countByStatus(DormApplicationStatus.PENDING);
        
        // Count open maintenance requests (PENDING + IN_PROGRESS)
        long openMaintenance = maintenanceRepository.count(
            (root, query, cb) -> root.get("maintenanceStatus").in(
                Arrays.asList(MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS)
            )
        );
        
        // Count unpaid invoices
        long unpaidInvoices = invoiceRepository.countByStatus(InvoiceStatus.UNPAID);
        
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .totalStudents(totalStudents)
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .pendingApplications(pendingApplications)
                .openMaintenance(openMaintenance)
                .unpaidInvoices(unpaidInvoices)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

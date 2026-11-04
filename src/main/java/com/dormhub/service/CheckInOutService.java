package com.dormhub.service;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomAssignmentMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInOutService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomRepository roomRepository;
    private final RoomAssignmentMapper roomAssignmentMapper;
    private final NotificationService notificationService;

    public CheckInOutService(ContractRepository contractRepository,
                             InvoiceRepository invoiceRepository,
                             RoomAssignmentRepository roomAssignmentRepository,
                             RoomRepository roomRepository,
                             RoomAssignmentMapper roomAssignmentMapper,
                             NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.roomRepository = roomRepository;
        this.roomAssignmentMapper = roomAssignmentMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public RoomAssignmentResponse checkIn(Long contractId) {
        Contract contract = getContractForUpdate(contractId);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ có thể check-in khi hợp đồng đang ACTIVE");
        }

        RoomAssignment assignment = getCurrentAssignmentForUpdate(contractId);
        if (assignment.getStatus() != RoomAssignmentStatus.ASSIGNED) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ có thể check-in khi phân phòng đang ASSIGNED");
        }

        assignment.setStatus(RoomAssignmentStatus.CHECKED_IN);
        assignment.setCheckInAt(LocalDateTime.now(BUSINESS_ZONE));
        
        // Send notification after check-in success
        if (contract.getStudent() != null && contract.getStudent().getUser() != null) {
            notificationService.sendNotification(
                NotificationType.GENERAL,
                "Check-in thành công",
                String.format("Bạn đã check-in thành công cho hợp đồng #%d", contractId),
                contractId,
                contract.getStudent().getUser().getId()
            );
        }
        
        return roomAssignmentMapper.toResponse(assignment);
    }

    @Transactional
    public RoomAssignmentResponse checkOut(Long contractId) {
        Contract contract = getContractForUpdate(contractId);
        if (contract.getStatus() != ContractStatus.ACTIVE
                && contract.getStatus() != ContractStatus.EXPIRED) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ có thể check-out hợp đồng ACTIVE hoặc EXPIRED");
        }

        RoomAssignment assignment = getCurrentAssignmentForUpdate(contractId);
        if (assignment.getStatus() != RoomAssignmentStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ có thể check-out khi phân phòng đang CHECKED_IN");
        }
        if (invoiceRepository.existsByContract_ContractIdAndStatus(
                contractId, InvoiceStatus.UNPAID)) {
            throw new AppException(ErrorCode.CONTRACT_HAS_UNPAID_INVOICES);
        }

        LocalDateTime checkOutAt = LocalDateTime.now(BUSINESS_ZONE);
        assignment.setStatus(RoomAssignmentStatus.CHECKED_OUT);
        assignment.setCheckOutAt(checkOutAt);
        if (contract.getStatus() != ContractStatus.EXPIRED) {
            contract.setStatus(contract.getEndDate() != null
                    && !contract.getEndDate().isAfter(checkOutAt)
                    ? ContractStatus.EXPIRED
                    : ContractStatus.TERMINATED);
        }
        refreshRoomAvailability(assignment.getRoom().getId());
        
        // Send notification after check-out success
        if (contract.getStudent() != null && contract.getStudent().getUser() != null) {
            notificationService.sendNotification(
                NotificationType.CHECKOUT_CONFIRMED,
                "Check-out thành công",
                String.format("Bạn đã check-out thành công cho hợp đồng #%d", contractId),
                contractId,
                contract.getStudent().getUser().getId()
            );
        }
        
        return roomAssignmentMapper.toResponse(assignment);
    }

    private Contract getContractForUpdate(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contractId phải là số dương");
        }
        return contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }

    private RoomAssignment getCurrentAssignmentForUpdate(Long contractId) {
        return roomAssignmentRepository
                .findFirstByContract_ContractIdOrderByAssignmentIdDesc(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    private void refreshRoomAvailability(Long roomId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        int remainingAssignments = Math.toIntExact(roomAssignmentRepository
                .countByRoom_IdAndStatusIn(roomId, List.of(RoomAssignmentStatus.CHECKED_IN)));
        room.setCurrentOccupancy(Math.max(Math.max(room.getCurrentOccupancy() - 1, 0),
                remainingAssignments));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            return;
        }

        long heldContracts = contractRepository.countByRoom_IdAndStatusIn(
                roomId, List.of(ContractStatus.PENDING));
        room.setStatus(room.getCurrentOccupancy() + heldContracts >= room.getCapacity()
                ? RoomStatus.RESERVED
                : RoomStatus.AVAILABLE);
    }
}

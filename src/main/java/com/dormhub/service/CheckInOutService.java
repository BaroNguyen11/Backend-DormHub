package com.dormhub.service;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
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
import java.time.LocalDate;
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
    private final RoomStatusService roomStatusService;

    public CheckInOutService(ContractRepository contractRepository,
                             InvoiceRepository invoiceRepository,
                             RoomAssignmentRepository roomAssignmentRepository,
                             RoomRepository roomRepository,
                             RoomAssignmentMapper roomAssignmentMapper,
                             NotificationService notificationService,
                             RoomStatusService roomStatusService) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.roomRepository = roomRepository;
        this.roomAssignmentMapper = roomAssignmentMapper;
        this.notificationService = notificationService;
        this.roomStatusService = roomStatusService;
    }

    @Transactional
    public RoomAssignmentResponse checkIn(Long contractId) {
        Contract contract = getContractForUpdate(contractId);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ có thể check-in khi hợp đồng đang ACTIVE");
        }
        if (contract.getDepositStatus() != DepositStatus.HELD
                || contract.getStartDate() == null
                || contract.getStartDate().toLocalDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chưa thanh toán cọc, chưa xác nhận ngày nhận phòng hoặc chưa đến ngày bắt đầu hợp đồng");
        }

        RoomAssignment assignment = getCurrentAssignmentForUpdate(contractId);
        if (assignment.getStatus() != RoomAssignmentStatus.ASSIGNED) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ có thể check-in khi phân phòng đang ASSIGNED");
        }
        if (assignment.getRoom() != null) {
            Room roomToCheckIn = roomRepository.findById(assignment.getRoom().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
            if (roomToCheckIn.getStatus() == RoomStatus.MAINTENANCE) {
                throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                        "Phòng đang bảo trì, không thể check-in");
            }
        }

        assignment.setStatus(RoomAssignmentStatus.CHECKED_IN);
        assignment.setCheckInAt(LocalDateTime.now(BUSINESS_ZONE));
        roomAssignmentRepository.saveAndFlush(assignment);
        if (contract.getStudent() != null) {
            contract.getStudent().setRoom(assignment.getRoom());
        }
        roomStatusService.syncOccupancyAndRefreshStatus(assignment.getRoom().getId());
        
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
                contractId, InvoiceStatus.UNPAID)
                || (assignment.getRoom() != null && invoiceRepository.existsByRoom_IdAndStatusAndInvoiceType(
                        assignment.getRoom().getId(), InvoiceStatus.UNPAID, InvoiceType.UTILITY))) {
            throw new AppException(ErrorCode.CONTRACT_HAS_UNPAID_INVOICES);
        }
        // Thống nhất 2 đường trả phòng: cọc đang HELD phải được quyết định (hoàn/tịch thu)
        // qua quy trình CheckoutRequest.complete() — không cho check-out trực tiếp
        // để cọc không bị kẹt lại trạng thái HELD (P1)
        if (contract.getDepositStatus() == com.dormhub.entity.enums.DepositStatus.HELD) {
            throw new AppException(ErrorCode.CHECKOUT_DEPOSIT_ACTION_REQUIRED,
                    "Hợp đồng còn tiền cọc đang giữ. Hãy hoàn tất qua quy trình yêu cầu trả phòng "
                            + "(checkout-requests) để chọn hoàn hoặc tịch thu tiền cọc");
        }

        LocalDateTime checkOutAt = LocalDateTime.now(BUSINESS_ZONE);
        Room roomBeingLeft = assignment.getRoom();
        assignment.setStatus(RoomAssignmentStatus.CHECKED_OUT);
        assignment.setCheckOutAt(checkOutAt);
        roomAssignmentRepository.saveAndFlush(assignment);

        if (contract.getStudent() != null) {
            Student student = contract.getStudent();
            RoomAssignment remainingAssignment = roomAssignmentRepository
                    .findFirstByStudent_IdAndStatusInOrderByAssignmentIdDesc(
                            student.getId(), List.of(
                                    RoomAssignmentStatus.ACTIVE,
                                    RoomAssignmentStatus.APPROVED,
                                    RoomAssignmentStatus.ASSIGNED,
                                    RoomAssignmentStatus.CHECKED_IN
                            ))
                    .orElse(null);
            student.setRoom(remainingAssignment == null ? null : remainingAssignment.getRoom());
        }

        if (contract.getStatus() != ContractStatus.EXPIRED) {
            contract.setStatus(contract.getEndDate() != null
                    && !contract.getEndDate().isAfter(checkOutAt)
                    ? ContractStatus.EXPIRED
                    : ContractStatus.TERMINATED);
            contractRepository.save(contract);
        }
        roomStatusService.syncOccupancyAndRefreshStatus(
                roomBeingLeft != null ? roomBeingLeft.getId() : assignment.getRoom().getId());
        
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
}

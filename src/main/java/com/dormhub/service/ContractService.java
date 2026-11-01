package com.dormhub.service;

import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int CONTRACT_DURATION_MONTHS = 12;

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final InvoiceService invoiceService;
    private final RoomAssignmentService roomAssignmentService;

    @Transactional
    public Contract createPendingForApprovedApplication(DormApplication application) {
        if (application.getStatus() != DormApplicationStatus.APPROVED) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_APPROVED);
        }
        if (contractRepository.findByDormApplicationId(application.getId()).isPresent()) {
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
        }
        if (contractRepository.existsByStudentIdAndStatusIn(application.getStudent().getId(),
                List.of(ContractStatus.PENDING, ContractStatus.ACTIVE))) {
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
        }

        Room room = roomRepository.findByIdForUpdate(application.getRequestedRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        ensureStudentMatchesBuilding(application, room);

        if (room.getStatus() == RoomStatus.MAINTENANCE || room.getStatus() == RoomStatus.INACTIVE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
        if (room.getStatus() == RoomStatus.FULL
                || room.getCurrentOccupancy() + room.getReservedOccupancy() >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        BigDecimal roomPrice = room.getPrice();
        if (roomPrice == null || roomPrice.signum() <= 0) {
            throw new AppException(ErrorCode.CONTRACT_RENT_INVALID);
        }
        BigDecimal monthlyRent = roomPrice.setScale(0, RoundingMode.HALF_UP);
        if (monthlyRent.signum() <= 0) {
            throw new AppException(ErrorCode.CONTRACT_RENT_INVALID);
        }

        LocalDate startDate = LocalDate.now(BUSINESS_ZONE);
        Contract contract = new Contract();
        contract.setDormApplication(application);
        contract.setStudent(application.getStudent());
        contract.setRoom(room);
        contract.setStartDate(startDate);
        contract.setEndDate(startDate.plusMonths(CONTRACT_DURATION_MONTHS));
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(monthlyRent);
        contract.setStatus(ContractStatus.PENDING);
        contract.setDescription("Hợp đồng nội trú 12 tháng được tạo từ đơn " + application.getId());

        Contract saved = contractRepository.saveAndFlush(contract);
        room.setReservedOccupancy(room.getReservedOccupancy() + 1);
        if (room.getCurrentOccupancy() + room.getReservedOccupancy() >= room.getCapacity()) {
            room.setStatus(RoomStatus.FULL);
        }
        roomRepository.save(room);
        invoiceService.createDepositInvoice(saved);
        return saved;
    }

    /** Called by the Payment module after it has marked the deposit invoice as paid. */
    @Transactional
    public void activateAfterDepositPayment(Long contractId) {
        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new AppException(ErrorCode.CONTRACT_NOT_PENDING);
        }

        Invoice depositInvoice = invoiceRepository
                .findByContract_ContractIdAndInvoiceType(contractId, InvoiceType.DEPOSIT)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID));
        if (depositInvoice.getStatus() != InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID);
        }

        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);
        roomAssignmentService.assign(contractId);
    }

    private void ensureStudentMatchesBuilding(DormApplication application, Room room) {
        BuildingGenderPolicy policy = room.getBuilding().getGenderPolicy();
        Gender gender = application.getStudent().getGender();
        if (policy == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (gender == Gender.OTHER || gender == null) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        boolean matches = policy == BuildingGenderPolicy.MALE_ONLY
                ? gender == Gender.MALE
                : gender == Gender.FEMALE;
        if (!matches) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
    }
}

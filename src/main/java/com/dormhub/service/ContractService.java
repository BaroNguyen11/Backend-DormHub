package com.dormhub.service;

import com.dormhub.dto.request.CreateContractRequest;
import com.dormhub.dto.request.UpdateContractRequest;
import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.ContractMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final List<ContractStatus> OPEN_CONTRACT_STATUSES =
            List.of(ContractStatus.PENDING, ContractStatus.ACTIVE);

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final ContractMapper contractMapper;
    private final DepositService depositService;

    public ContractService(ContractRepository contractRepository, RoomRepository roomRepository,
                           StudentRepository studentRepository, ContractMapper contractMapper,
                           DepositService depositService) {
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.contractMapper = contractMapper;
        this.depositService = depositService;
    }

    @Transactional
    public ContractResponse create(CreateContractRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        if (contractRepository.existsByStudent_IdAndStatusIn(
                request.getStudentId(), OPEN_CONTRACT_STATUSES)) {
            throw new AppException(ErrorCode.CONTRACT_CONFLICT,
                    "Sinh viên đã có hợp đồng PENDING hoặc ACTIVE");
        }

        Contract contract = contractMapper.toEntity(request, room, student);
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    @Transactional
    public Contract createPendingForApprovedApplication(DormApplication application) {
        if (application.getStatus() != DormApplicationStatus.APPROVED) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_APPROVED);
        }
        if (contractRepository.findByDormApplication_Id(application.getId()).isPresent()) {
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
        }

        Student student = studentRepository.findByUserIdForUpdate(
                        application.getStudent().getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        if (contractRepository.existsByStudent_IdAndStatusIn(student.getId(), OPEN_CONTRACT_STATUSES)) {
            throw new AppException(ErrorCode.CONTRACT_CONFLICT,
                    "Sinh viên đã có hợp đồng PENDING hoặc ACTIVE");
        }

        Room room = roomRepository.findByIdForUpdate(application.getRequestedRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        BuildingGenderPolicy policy = room.getBuilding().getGenderPolicy();
        Gender gender = student.getGender();
        if (policy == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (gender == null || gender == Gender.OTHER) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        boolean genderMatches = policy == BuildingGenderPolicy.MALE_ONLY
                ? gender == Gender.MALE
                : gender == Gender.FEMALE;
        if (!genderMatches) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
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

        BigDecimal price = room.getPrice();
        if (price == null || price.signum() <= 0) {
            throw new AppException(ErrorCode.CONTRACT_RENT_INVALID);
        }
        BigDecimal monthlyRent = price.setScale(0, RoundingMode.HALF_UP);
        if (monthlyRent.signum() <= 0) {
            throw new AppException(ErrorCode.CONTRACT_RENT_INVALID);
        }

        LocalDateTime startDate = application.getProcessedAt() == null
                ? LocalDateTime.now(BUSINESS_ZONE)
                : application.getProcessedAt();
        Contract contract = new Contract();
        contract.setDormApplication(application);
        contract.setRoom(room);
        contract.setStudent(student);
        contract.setStartDate(startDate);
        contract.setEndDate(startDate.plusMonths(12));
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(monthlyRent);
        contract.setDepositStatus(DepositStatus.UNPAID);
        contract.setStatus(ContractStatus.PENDING);
        contract.setDescription("Hợp đồng nội trú 12 tháng từ đơn " + application.getId());

        Contract saved = contractRepository.saveAndFlush(contract);
        room.setReservedOccupancy(room.getReservedOccupancy() + 1);
        if (room.getCurrentOccupancy() + room.getReservedOccupancy() >= room.getCapacity()) {
            room.setStatus(RoomStatus.FULL);
        }
        roomRepository.save(room);
        depositService.createDepositInvoice(saved.getContractId());
        return saved;
    }

    @Transactional(readOnly = true)
    public ContractResponse getById(Long contractId) {
        return contractMapper.toResponse(findContract(contractId));
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> search(Long studentId, ContractStatus status, Pageable pageable) {
        Page<Contract> contracts;
        if (studentId != null && status != null) {
            contracts = contractRepository.findAllByStudent_IdAndStatus(studentId, status, pageable);
        } else if (studentId != null) {
            contracts = contractRepository.findAllByStudent_Id(studentId, pageable);
        } else if (status != null) {
            contracts = contractRepository.findAllByStatus(status, pageable);
        } else {
            contracts = contractRepository.findAll(pageable);
        }
        return contracts.map(contractMapper::toResponse);
    }

    @Transactional
    public ContractResponse updatePending(Long contractId, UpdateContractRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Contract contract = findContract(contractId);
        ensurePending(contract, "Chỉ có thể cập nhật hợp đồng PENDING");
        contract.updatePending(request.getStartDate(), request.getEndDate(),
                request.getMonthlyRent(), request.getDepositAmount(), request.getDescription());
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    @Transactional
    public ContractResponse cancelPending(Long contractId) {
        Contract contract = findContract(contractId);
        ensurePending(contract, "Chỉ có thể hủy hợp đồng PENDING");
        contract.cancelPending();
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    private Contract findContract(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contractId phải là số dương");
        }
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }

    private void ensurePending(Contract contract, String message) {
        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new AppException(ErrorCode.CONTRACT_CONFLICT, message);
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new AppException(ErrorCode.CONTRACT_INVALID,
                    "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }
}

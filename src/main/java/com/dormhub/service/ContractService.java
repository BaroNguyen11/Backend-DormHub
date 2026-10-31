package com.dormhub.service;

import com.dormhub.dto.request.CreateContractRequest;
import com.dormhub.dto.request.UpdateContractRequest;
import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.ContractMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

    private static final List<ContractStatus> OPEN_CONTRACT_STATUSES =
            List.of(ContractStatus.PENDING, ContractStatus.ACTIVE);

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final ContractMapper contractMapper;

    public ContractService(ContractRepository contractRepository, RoomRepository roomRepository,
                           StudentRepository studentRepository, ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.contractMapper = contractMapper;
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

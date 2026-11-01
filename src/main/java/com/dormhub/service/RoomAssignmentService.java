package com.dormhub.service;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomAssignmentMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomAssignmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RoomAssignmentRepository roomAssignmentRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final StudentRepository studentRepository;
    private final RoomAssignmentMapper roomAssignmentMapper;

    @Transactional
    public RoomAssignmentResponse assign(Long contractId) {
        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        Invoice depositInvoice = invoiceRepository
                .findByContract_ContractIdAndInvoiceType(contractId, InvoiceType.DEPOSIT)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID));
        if (depositInvoice.getStatus() != InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID);
        }
        DormApplication application = contract.getDormApplication();
        if (application == null) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_FOUND);
        }
        if (application.getStatus() != DormApplicationStatus.APPROVED) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_APPROVED);
        }
        if (contract.getStudent() == null || contract.getRoom() == null) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }

        Student student = studentRepository.findByUserIdForUpdate(contract.getStudent().getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        if (roomAssignmentRepository.existsByDormApplicationStudentIdAndStatus(
                student.getId(), RoomAssignmentStatus.ASSIGNED)) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_ASSIGNED);
        }
        if (roomAssignmentRepository.existsByContractContractIdAndStatus(
                contractId, RoomAssignmentStatus.ASSIGNED)) {
            throw new AppException(ErrorCode.DORM_APPLICATION_ALREADY_ASSIGNED);
        }

        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Building building = buildingRepository.findByIdForUpdate(room.getBuilding().getId())
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));

        ensureGenderMatches(student.getGender(), building.getGenderPolicy());

        if (room.getReservedOccupancy() <= 0
                || room.getCurrentOccupancy() + room.getReservedOccupancy() > room.getCapacity()
                || room.getCurrentOccupancy() >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE || room.getStatus() == RoomStatus.INACTIVE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        RoomAssignment assignment = new RoomAssignment();
        assignment.setDormApplication(application);
        assignment.setContract(contract);
        assignment.setRoom(room);
        assignment.setStatus(RoomAssignmentStatus.ASSIGNED);
        assignment.setNote(application.getNote());

        room.setReservedOccupancy(room.getReservedOccupancy() - 1);
        room.setCurrentOccupancy(room.getCurrentOccupancy() + 1);
        if (room.getCurrentOccupancy() + room.getReservedOccupancy() >= room.getCapacity()) {
            room.setStatus(RoomStatus.FULL);
        } else {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        roomRepository.save(room);

        return roomAssignmentMapper.toResponse(roomAssignmentRepository.save(assignment));
    }

    public Page<RoomAssignmentResponse> getMyAssignments(Long userId, int page, int size) {
        validatePage(page, size);
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        return roomAssignmentRepository.findAllByDormApplicationStudentId(
                        student.getId(), pageable(page, size))
                .map(roomAssignmentMapper::toResponse);
    }

    public Page<RoomAssignmentResponse> getAll(
            RoomAssignmentStatus status,
            int page,
            int size) {
        validatePage(page, size);
        Pageable pageable = pageable(page, size);
        Page<RoomAssignment> assignments = status == null
                ? roomAssignmentRepository.findAll(pageable)
                : roomAssignmentRepository.findAllByStatus(status, pageable);
        return assignments.map(roomAssignmentMapper::toResponse);
    }

    public RoomAssignmentResponse getMyAssignment(Long userId, Long assignmentId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        return roomAssignmentRepository.findByIdAndDormApplicationStudentId(assignmentId, student.getId())
                .map(roomAssignmentMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    public RoomAssignmentResponse getById(Long assignmentId) {
        return roomAssignmentRepository.findById(assignmentId)
                .map(roomAssignmentMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    private void ensureGenderMatches(Gender studentGender, BuildingGenderPolicy policy) {
        if (policy == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (studentGender == Gender.OTHER) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        boolean matches = policy == BuildingGenderPolicy.MALE_ONLY
                ? studentGender == Gender.MALE
                : studentGender == Gender.FEMALE;
        if (!matches) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }
}

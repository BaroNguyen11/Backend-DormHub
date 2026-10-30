package com.dormhub.service;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.DormApplicationRepository;
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
    private final DormApplicationRepository dormApplicationRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public RoomAssignmentResponse assign(Long dormApplicationId) {
        DormApplication application = dormApplicationRepository.findByIdForUpdate(dormApplicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));
        if (application.getStatus() != DormApplicationStatus.APPROVED) {
            throw new RuntimeException("Chỉ có thể phân phòng cho đơn đã được duyệt");
        }
        if (application.getRequestedRoom() == null) {
            throw new RuntimeException("Chưa có chính sách phân phòng phù hợp với hồ sơ sinh viên này");
        }

        Student student = studentRepository.findByUserIdForUpdate(application.getStudent().getUser().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ sinh viên"));
        if (roomAssignmentRepository.existsByDormApplicationStudentIdAndStatus(
                student.getId(), RoomAssignmentStatus.ASSIGNED)) {
            throw new RuntimeException("Sinh viên đã được phân phòng");
        }
        if (roomAssignmentRepository.existsByDormApplicationIdAndStatus(
                dormApplicationId, RoomAssignmentStatus.ASSIGNED)) {
            throw new RuntimeException("Đơn đăng ký đã có phân phòng đang hiệu lực");
        }

        Room room = roomRepository.findByIdForUpdate(application.getRequestedRoom().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng đã chọn"));
        Building building = buildingRepository.findByIdForUpdate(room.getBuilding().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà"));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RuntimeException("Chỉ có thể phân phòng đang hoạt động");
        }
        ensureGenderMatches(student.getGender(), building.getGenderPolicy());

        long occupiedSlots = roomAssignmentRepository.countByRoomIdAndStatus(
                room.getId(), RoomAssignmentStatus.ASSIGNED);
        if (occupiedSlots >= room.getCapacity()) {
            throw new RuntimeException("Phòng đã đủ sức chứa");
        }

        RoomAssignment assignment = new RoomAssignment();
        assignment.setDormApplication(application);
        assignment.setRoom(room);
        assignment.setStatus(RoomAssignmentStatus.ASSIGNED);
        assignment.setNote(application.getNote());

        return RoomAssignmentResponse.from(roomAssignmentRepository.save(assignment));
    }

    public Page<RoomAssignmentResponse> getMyAssignments(Long userId, int page, int size) {
        validatePage(page, size);
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ sinh viên"));
        return roomAssignmentRepository.findAllByDormApplicationStudentId(
                        student.getId(), pageable(page, size))
                .map(RoomAssignmentResponse::from);
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
        return assignments.map(RoomAssignmentResponse::from);
    }

    public RoomAssignmentResponse getMyAssignment(Long userId, Long assignmentId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ sinh viên"));
        return roomAssignmentRepository.findByIdAndDormApplicationStudentId(assignmentId, student.getId())
                .map(RoomAssignmentResponse::from)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân phòng"));
    }

    public RoomAssignmentResponse getById(Long assignmentId) {
        return roomAssignmentRepository.findById(assignmentId)
                .map(RoomAssignmentResponse::from)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân phòng"));
    }

    private void ensureGenderMatches(Gender studentGender, BuildingGenderPolicy policy) {
        if (policy == null) {
            throw new RuntimeException("Tòa nhà chưa được phân loại theo giới tính");
        }
        if (studentGender == Gender.OTHER) {
            throw new RuntimeException("Chưa thể tự động phân phòng cho hồ sơ có giới tính OTHER");
        }
        boolean matches = policy == BuildingGenderPolicy.MALE_ONLY
                ? studentGender == Gender.MALE
                : studentGender == Gender.FEMALE;
        if (!matches) {
            throw new RuntimeException("Giới tính sinh viên không phù hợp với chính sách của tòa nhà");
        }
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new RuntimeException("page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }
}

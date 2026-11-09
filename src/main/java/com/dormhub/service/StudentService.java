
package com.dormhub.service;

import com.dormhub.dto.request.StudentProfileUpdateRequest;
import com.dormhub.dto.response.StudentProfileResponse;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.UserStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.StudentMapper;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.specification.StudentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final StudentMapper studentMapper;
    private final RefreshTokenService refreshTokenService;

    public StudentProfileResponse getMyProfile(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.STUDENT_NOT_FOUND));

        return studentMapper.toResponse(student);
    }

    @Transactional
    public StudentProfileResponse updateMyProfile(
            Long userId,
            StudentProfileUpdateRequest request) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.STUDENT_NOT_FOUND));

        // Bổ sung mã sinh viên MỘT LẦN DUY NHẤT khi hồ sơ còn trống mã
        if (StringUtils.hasText(request.getStudentCode())) {
            String incomingCode = request.getStudentCode().trim();
            if (StringUtils.hasText(student.getStudentCode())) {
                if (!incomingCode.equals(student.getStudentCode())) {
                    throw new AppException(ErrorCode.INVALID_REQUEST,
                            "Mã sinh viên đã được cấp và không thể thay đổi. Vui lòng liên hệ ban quản lý KTX.");
                }
            } else if (studentRepository.existsByStudentCode(incomingCode)) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Mã sinh viên này đã thuộc về một tài khoản khác. Vui lòng kiểm tra lại.");
            } else {
                student.setStudentCode(incomingCode);
            }
        }

        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null && request.getGender() != student.getGender()) {
            // BR §5.2: chặn đổi giới tính khi đang lưu trú tại tòa áp giới tính
            boolean hasActiveStay = roomAssignmentRepository.existsByStudent_IdAndStatusIn(
                    student.getId(), List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN));
            if (hasActiveStay) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Không thể đổi giới tính khi đang lưu trú tại ký túc xá. Hãy liên hệ quản lý KTX");
            }
            student.setGender(request.getGender());
        }

        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }

        if (request.getAvatarUrl() != null) {
            student.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null){
            student.getUser().setPhone(request.getPhone());
        }
        if (request.getCic() != null) {
            student.setCic(request.getCic());
        }

        studentRepository.save(student);

        return getMyProfile(userId);
    }
    public Page<StudentProfileResponse> searchStudent(
            String name,
            String studentCode,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        // 1. Validate pagination
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        // 2. Fallback giá trị mặc định an toàn nếu null
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "studentCode" : sortBy.trim();
        String safeDirection = (direction == null || direction.isBlank()) ? "ASC" : direction.trim().toUpperCase();
        // 3. Whitelist các trường được phép sắp xếp
        if (!Set.of("fullName", "studentCode", "id").contains(safeSortBy)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (!safeDirection.equals("ASC") && !safeDirection.equals("DESC")) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Sort.Direction sortDirection = Sort.Direction.fromString(safeDirection);
        // 4. Map tên trường phía Client sang JPA Property Path chính xác
        String jpaSortProperty = switch (safeSortBy) {
            case "fullName" -> "user.fullName";
            case "studentCode" -> "studentCode";
            default -> "id";
        };
        Sort sort = Sort.by(sortDirection, jpaSortProperty);
        // Thêm sort phụ theo ID để đảm bảo thứ tự phân trang ổn định tuyệt đối
        if (!safeSortBy.equals("id")) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        // 5. Kết hợp Specification tìm kiếm
        Specification<Student> spec = StudentSpecification.hasName(name).
                and(StudentSpecification.hasStudentCode(studentCode));
        // 6. Truy vấn và map sang Response
        Page<Student> students = studentRepository.findAll(spec, pageable);
        return students.map(studentMapper::toResponse);
    }
    public StudentProfileResponse getStudentById(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        return studentMapper.toResponse(student);
    }
    @Transactional
    public StudentProfileResponse updateStudentStatus(Long studentId, UserStatus newStatus) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        if (newStatus != UserStatus.ACTIVE && newStatus != UserStatus.LOCKED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        boolean statusChanged = student.getUser().getStatus() != newStatus;
        student.getUser().setStatus(newStatus);
        studentRepository.save(student);

        // Khóa tài khoản phải revoke refresh token đang hiệu lực để phiên đăng nhập chết ngay
        if (statusChanged && newStatus == UserStatus.LOCKED) {
            refreshTokenService.revokeAllForUser(student.getUser().getId());
        }

        return studentMapper.toResponse(student);
    }
}

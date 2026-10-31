
package com.dormhub.service;

import com.dormhub.dto.request.StudentProfileUpdateRequest;
import com.dormhub.dto.response.StudentProfileResponse;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.UserStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.StudentMapper;
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

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

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

        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
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

        student.getUser().setStatus(newStatus);
        studentRepository.save(student);

        return studentMapper.toResponse(student);
    }
}

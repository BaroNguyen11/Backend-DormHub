
package com.dormhub.controller;

import com.dormhub.dto.request.StudentProfileUpdateRequest;
import com.dormhub.dto.request.StudentStatusUpdateRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.StudentProfileResponse;
import com.dormhub.entity.User;
import com.dormhub.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/student/profile")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.success(studentService.getMyProfile(user.getId())));
    }

    @PatchMapping("/student/profile")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StudentProfileUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(studentService.updateMyProfile(user.getId(), request)));
    }
    @GetMapping("/manager/students")
    public ResponseEntity<ApiResponse<Page<StudentProfileResponse>>> searchStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Page<StudentProfileResponse> result = studentService.searchStudent(
                name, studentCode, page, size, sortBy, direction
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sinh viên thành công", result));
    }
    @GetMapping("/manager/students/{studentId}")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getStudentById(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin sinh viên thành công",
                studentService.getStudentById(studentId)
        ));
    }
    @PatchMapping("/manager/students/{studentId}/status")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateStudentStatus(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentStatusUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái sinh viên thành công",
                studentService.updateStudentStatus(studentId, request.getStatus())
        ));
    }
}

package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.Gender;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.StudentRepository;
import com.dormhub.service.RoomService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P1: Sinh viên xem danh sách phòng trống (đúng giới tính tòa, còn chỗ thật)
 * để tự chọn phòng khi nộp đơn đăng ký lưu trú.
 */
@RestController
@RequestMapping("/api/v1/student/rooms-available")
@RequiredArgsConstructor
public class StudentRoomController {

    private final RoomService roomService;
    private final StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRoomsForMe(
            @AuthenticationPrincipal User user) {
        
        // Lấy student profile nếu có
        Optional<Student> studentOpt = studentRepository.findByUserId(user.getId());
        
        List<RoomResponse> rooms;
        
        if (studentOpt.isEmpty() || studentOpt.get().getGender() == null 
                || (studentOpt.get().getGender() != Gender.MALE 
                    && studentOpt.get().getGender() != Gender.FEMALE)) {
            // Chưa có profile hoặc chưa khai báo giới tính hợp lệ
            // → Trả về tất cả phòng trống
            rooms = roomService.getAvailableRooms();
            
            return ResponseEntity.ok(ApiResponse.success(
                "Vui lòng hoàn thiện hồ sơ (giới tính) tại trang Hồ sơ để xem phòng phù hợp. " +
                "Hiện tại hiển thị tất cả phòng trống.", 
                rooms
            ));
        }
        
        // Student đã có profile đầy đủ
        Student student = studentOpt.get();
        rooms = roomService.getAvailableRoomsForStudent(student.getGender());
        
        return ResponseEntity.ok(ApiResponse.success(
            "Lấy danh sách phòng trống theo giới tính của bạn thành công", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin phòng thành công", room));
    }
}

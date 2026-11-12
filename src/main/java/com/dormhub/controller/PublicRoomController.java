package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.Role;
import com.dormhub.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public API cho guest xem phòng trống; Student đăng nhập chỉ thấy phòng đúng giới tính.
 */
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class PublicRoomController {

    private final RoomService roomService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getPublicAvailableRooms(
            @AuthenticationPrincipal User user) {
        List<RoomResponse> rooms = user != null && user.getRole() == Role.STUDENT
                ? roomService.getAvailableRoomsForStudent(user)
                : roomService.getAvailableRooms();
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách phòng trống thành công", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getPublicRoomById(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        RoomResponse room = user != null && user.getRole() == Role.STUDENT
                ? roomService.getRoomByIdForStudent(id, user)
                : roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng thành công", room));
    }
}

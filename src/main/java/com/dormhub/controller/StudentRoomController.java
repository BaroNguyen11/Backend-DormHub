package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ContractDraftResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.User;
import com.dormhub.service.RoomService;
import com.dormhub.service.ContractDraftService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
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
    private final ContractDraftService contractDraftService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRoomsForMe(
            @AuthenticationPrincipal User user) {
        
        List<RoomResponse> rooms = roomService.getAvailableRoomsForStudent(user);
        
        return ResponseEntity.ok(ApiResponse.success(
            "Lấy danh sách phòng trống theo giới tính của bạn thành công", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        RoomResponse room = roomService.getRoomByIdForStudent(id, user);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin phòng thành công", room));
    }

    @GetMapping("/{id}/contract-draft")
    public ResponseEntity<ApiResponse<ContractDraftResponse>> getContractDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(contractDraftService.getDraft(user, id)));
    }
}

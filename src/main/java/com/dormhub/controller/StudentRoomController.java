package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.StudentRoomResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import com.dormhub.service.StudentRoomCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/rooms")
public class StudentRoomController {

    private final StudentRoomCatalogService roomCatalogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StudentRoomResponse>>> getRooms(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(roomCatalogService.getRooms(
                user.getId(), search, status, roomType, buildingId, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentRoomResponse>> getRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roomCatalogService.getRoom(user.getId(), id)));
    }
}

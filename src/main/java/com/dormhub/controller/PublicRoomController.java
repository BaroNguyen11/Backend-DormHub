package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import com.dormhub.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class PublicRoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getRooms(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "roomNumber") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<RoomResponse> rooms = roomService.getAllRooms(status, roomType, buildingId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng thành công", rooms));
    }

  
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms() {
        List<RoomResponse> rooms = roomService.getAvailableRooms();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng còn trống thành công", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng thành công", room));
    }
}
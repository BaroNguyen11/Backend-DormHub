package com.dormhub.controller;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import com.dormhub.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/rooms")
@RequiredArgsConstructor
public class RoomController {
    
    private final RoomService roomService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getAllRooms(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<RoomResponse> rooms = roomService.getAllRooms(status, roomType, buildingId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Get rooms successfully", rooms));
    }
    
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms() {
        List<RoomResponse> rooms = roomService.getAvailableRooms();
        return ResponseEntity.ok(ApiResponse.success("Get available rooms successfully", rooms));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Get room successfully", room));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", room));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", room));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable Long id,
            @RequestParam RoomStatus status) {
        RoomResponse room = roomService.updateRoomStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Room status updated successfully", room));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }
}

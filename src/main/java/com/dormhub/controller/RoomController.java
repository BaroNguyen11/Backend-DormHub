package com.dormhub.controller;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> search(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) BuildingGenderPolicy buildingGenderPolicy,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(roomService.search(
                buildingId, buildingGenderPolicy, roomType, status, availableOnly, keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> create(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phòng thành công", roomService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng thành công", roomService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng thành công", null));
    }
}

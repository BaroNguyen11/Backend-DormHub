package com.dormhub.controller;

import com.dormhub.dto.request.SemanticRoomSearchRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.dto.response.RoomReindexResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.dto.response.SemanticRoomSearchAnalysisResponse;
import com.dormhub.dto.response.SemanticRoomSearchResponse;
import com.dormhub.service.SemanticRoomSearchService;
import com.dormhub.service.BuildingService;
import com.dormhub.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SemanticRoomSearchController {
    private final SemanticRoomSearchService semanticRoomSearchService;
    private final BuildingService buildingService;
    private final RoomService roomService;

    @GetMapping("/student/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> rooms() {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomsForStudent()));
    }

    @GetMapping("/student/rooms/buildings")
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> buildings() {
        return ResponseEntity.ok(ApiResponse.success(buildingService.getAllBuildings()));
    }

    /** Nhận truy vấn đã xác thực của sinh viên và trả Room kèm điểm tương đồng. */
    @PostMapping("/student/rooms/semantic-search")
    public ResponseEntity<ApiResponse<List<SemanticRoomSearchResponse>>> search(
            @Valid @RequestBody SemanticRoomSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(semanticRoomSearchService.search(request)));
    }

    /** Trả danh sách giới hạn và thống kê trên toàn bộ phòng hợp lệ của cùng lần tìm vector. */
    @PostMapping("/student/rooms/semantic-search/analysis")
    public ResponseEntity<ApiResponse<SemanticRoomSearchAnalysisResponse>> analyze(
            @Valid @RequestBody SemanticRoomSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(semanticRoomSearchService.analyze(request)));
    }

    /** Cho quản lý yêu cầu tạo lại chỉ mục vector từ dữ liệu Room hiện có. */
    @PostMapping("/manager/rooms/semantic-index/reindex")
    public ResponseEntity<ApiResponse<RoomReindexResponse>> reindex() {
        return ResponseEntity.ok(ApiResponse.success(semanticRoomSearchService.reindex()));
    }
}

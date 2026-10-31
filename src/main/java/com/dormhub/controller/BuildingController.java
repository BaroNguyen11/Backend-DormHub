package com.dormhub.controller;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.service.BuildingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(buildingService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(buildingService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BuildingResponse>> create(@Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tòa nhà thành công", buildingService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật tòa nhà thành công", buildingService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tòa nhà thành công", null));
    }
}

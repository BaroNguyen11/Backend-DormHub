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
@RequestMapping("/api/v1/manager/buildings")
@RequiredArgsConstructor
public class BuildingController {
    
    private final BuildingService buildingService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> getAllBuildings() {
        List<BuildingResponse> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok(ApiResponse.success("Get buildings successfully", buildings));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponse>> getBuildingById(@PathVariable Long id) {
        BuildingResponse building = buildingService.getBuildingById(id);
        return ResponseEntity.ok(ApiResponse.success("Get building successfully", building));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<BuildingResponse>> createBuilding(
            @Valid @RequestBody BuildingRequest request) {
        BuildingResponse building = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Building created successfully", building));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponse>> updateBuilding(
            @PathVariable Long id,
            @Valid @RequestBody BuildingRequest request) {
        BuildingResponse building = buildingService.updateBuilding(id, request);
        return ResponseEntity.ok(ApiResponse.success("Building updated successfully", building));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBuilding(@PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.ok(ApiResponse.success("Building deleted successfully", null));
    }
}

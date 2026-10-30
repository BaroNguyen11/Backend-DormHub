package com.dormhub.controller;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.service.BuildingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    public List<BuildingResponse> getAll() {
        return buildingService.getAll();
    }

    @GetMapping("/{id}")
    public BuildingResponse getById(@PathVariable Long id) {
        return buildingService.getById(id);
    }

    @PostMapping
    public BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        return buildingService.create(request);
    }

    @PutMapping("/{id}")
    public BuildingResponse update(@PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        return buildingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        buildingService.delete(id);
    }
}

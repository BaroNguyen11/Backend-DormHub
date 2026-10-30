package com.dormhub.controller;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public Page<RoomResponse> search(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) BuildingGenderPolicy buildingGenderPolicy,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return roomService.search(
                buildingId, buildingGenderPolicy, roomType, status, availableOnly, keyword, page, size);
    }

    @GetMapping("/{id}")
    public RoomResponse getById(@PathVariable Long id) {
        return roomService.getById(id);
    }

    @PostMapping
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}

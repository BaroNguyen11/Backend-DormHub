package com.dormhub.controller;

import com.dormhub.dto.request.DormApplicationRequest;
import com.dormhub.dto.request.DormApplicationRoomChangeRequest;
import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.User;
import com.dormhub.service.DormApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/dorm-applications")
public class DormApplicationController {

    private final DormApplicationService dormApplicationService;

    @PostMapping
    public DormApplicationResponse submit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DormApplicationRequest request) {
        return dormApplicationService.submit(user.getId(), request);
    }

    @GetMapping
    public Page<DormApplicationResponse> getMyApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return dormApplicationService.getMyApplications(user.getId(), page, size);
    }

    @GetMapping("/{id}")
    public DormApplicationResponse getMyApplication(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return dormApplicationService.getMyApplication(user.getId(), id);
    }

    @PatchMapping("/{id}/room")
    public DormApplicationResponse changeRequestedRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody DormApplicationRoomChangeRequest request) {
        return dormApplicationService.changeRequestedRoom(user.getId(), id, request.getRoomId());
    }

    @PatchMapping("/{id}/cancel")
    public DormApplicationResponse cancel(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return dormApplicationService.cancel(user.getId(), id);
    }
}

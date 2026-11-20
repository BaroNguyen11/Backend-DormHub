package com.dormhub.controller;

import com.dormhub.dto.request.AiChatRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.entity.User;
import com.dormhub.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(
            @RequestBody AiChatRequest request,
            @AuthenticationPrincipal User user
    ) {
        String fullName = user != null ? user.getFullName() : null;
        String role = user != null && user.getRole() != null ? user.getRole().name() : null;
        String reply = aiChatService.chat(request, fullName, role);
        return ResponseEntity.ok(ApiResponse.success("Phản hồi từ trợ lý ảo AI", reply));
    }
}

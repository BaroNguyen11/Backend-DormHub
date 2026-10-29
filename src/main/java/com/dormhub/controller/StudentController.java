
package com.dormhub.controller;

import com.dormhub.dto.request.StudentProfileUpdateRequest;
import com.dormhub.dto.response.StudentProfileResponse;
import com.dormhub.entity.User;
import com.dormhub.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/profile")
    public StudentProfileResponse getMyProfile(
            @AuthenticationPrincipal User user) {

        return studentService.getMyProfile(user.getId());
    }

    @PatchMapping("/profile")
    public StudentProfileResponse updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StudentProfileUpdateRequest request) {

        return studentService.updateMyProfile(user.getId(), request);
    }

}

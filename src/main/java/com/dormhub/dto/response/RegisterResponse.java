package com.dormhub.dto.response;

import com.dormhub.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private final Long userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final UserStatus status;
}
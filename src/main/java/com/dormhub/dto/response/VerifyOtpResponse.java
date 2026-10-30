package com.dormhub.dto.response;

import com.dormhub.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyOtpResponse {

    private String email;
    private UserStatus status;
}
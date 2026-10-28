
package com.dormhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {

    private final LoginResponse user;
    private final String accessToken;
    private String refreshToken;
}

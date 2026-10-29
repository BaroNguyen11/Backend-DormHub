
package com.dormhub.dto.response;

import com.dormhub.entity.enums.Role;
import com.dormhub.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private UserStatus status;
}


package com.dormhub.dto.response;

import com.dormhub.entity.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StudentProfileResponse {

    private final Long studentId;
    private final String studentCode;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String address;
    private final String avatarUrl;
    private final String phone;
    private final String cic;
    private final String username;
    private final String email;
    private final String fullName;
}

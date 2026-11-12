package com.dormhub.dto.response;

import com.dormhub.entity.enums.Gender;

public record RoommateResponse(String fullName, String studentCode, Gender gender) {
}

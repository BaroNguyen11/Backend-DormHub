package com.dormhub.dto.response;

import com.dormhub.entity.enums.Gender;

public record RoomResidentResponse(Long studentId, String fullName, String studentCode, Gender gender) {
}

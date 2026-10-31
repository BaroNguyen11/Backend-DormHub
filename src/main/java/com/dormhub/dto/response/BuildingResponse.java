package com.dormhub.dto.response;

import com.dormhub.entity.enums.BuildingGenderPolicy;
import java.time.LocalDateTime;

public record BuildingResponse(
        Long id,
        String buildingName,
        String address,
        BuildingGenderPolicy genderPolicy,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

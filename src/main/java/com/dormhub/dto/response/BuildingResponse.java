package com.dormhub.dto.response;

import com.dormhub.entity.Building;
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

    public static BuildingResponse from(Building building) {
        return new BuildingResponse(
                building.getId(),
                building.getBuildingName(),
                building.getAddress(),
                building.getGenderPolicy(),
                building.getDescription(),
                building.getCreatedAt(),
                building.getUpdatedAt());
    }
}

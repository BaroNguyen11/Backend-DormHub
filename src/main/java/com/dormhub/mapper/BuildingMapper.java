package com.dormhub.mapper;

import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.entity.Building;
import org.springframework.stereotype.Component;

@Component
public class BuildingMapper {

    public BuildingResponse toResponse(Building building) {
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

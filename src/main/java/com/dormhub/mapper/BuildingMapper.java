package com.dormhub.mapper;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.entity.Building;
import org.springframework.stereotype.Component;

@Component
public class BuildingMapper {
    
    public Building toEntity(BuildingRequest request) {
        return Building.builder()
                .buildingCode(request.getBuildingCode())
                .name(request.getName())
                .address(request.getAddress())
                .totalFloors(request.getTotalFloors())
                .genderRestriction(request.getGenderRestriction())
                .description(request.getDescription())
                .build();
    }
    
    public BuildingResponse toResponse(Building building, Long totalRooms, Long availableRooms) {
        return BuildingResponse.builder()
                .id(building.getId())
                .buildingCode(building.getBuildingCode())
                .name(building.getName())
                .address(building.getAddress())
                .totalFloors(building.getTotalFloors())
                .genderRestriction(building.getGenderRestriction())
                .description(building.getDescription())
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .createdAt(building.getCreatedAt())
                .updatedAt(building.getUpdatedAt())
                .build();
    }
}

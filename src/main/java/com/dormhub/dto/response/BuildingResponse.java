package com.dormhub.dto.response;

import com.dormhub.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingResponse {
    private Long id;
    private String buildingCode;
    private String name;
    private String address;
    private Integer totalFloors;
    private Gender genderRestriction;
    private String description;
    private Long totalRooms;
    private Long availableRooms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

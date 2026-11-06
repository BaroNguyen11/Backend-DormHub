package com.dormhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BuildingRoomCountResponse {
    private Long buildingId;
    private String buildingName;
    private long count;
}

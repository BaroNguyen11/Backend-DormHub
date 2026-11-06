package com.dormhub.dto.response;

import com.dormhub.entity.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class RoomSearchStatisticsResponse {
    private int totalRooms;
    private BigDecimal averagePrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Map<RoomType, Long> roomsByType;
    private List<BuildingRoomCountResponse> roomsByBuilding;
}

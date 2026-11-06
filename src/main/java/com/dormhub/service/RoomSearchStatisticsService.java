package com.dormhub.service;

import com.dormhub.dto.response.BuildingRoomCountResponse;
import com.dormhub.dto.response.RoomSearchStatisticsResponse;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.RoomType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class RoomSearchStatisticsService {
    /** Tính thống kê trên toàn bộ phòng hợp lệ, trước khi giới hạn danh sách hiển thị. */
    public RoomSearchStatisticsResponse calculate(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return new RoomSearchStatisticsResponse(0, null, null, null, Map.of(), List.of());
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        Map<RoomType, Long> byType = new EnumMap<>(RoomType.class);
        Map<Long, Long> byBuilding = new TreeMap<>();
        Map<Long, String> buildingNames = new TreeMap<>();
        for (Room room : rooms) {
            BigDecimal price = room.getPricePerMonth();
            sum = sum.add(price);
            min = min == null || price.compareTo(min) < 0 ? price : min;
            max = max == null || price.compareTo(max) > 0 ? price : max;
            byType.merge(room.getRoomType(), 1L, Long::sum);
            Long buildingId = room.getBuilding().getId();
            byBuilding.merge(buildingId, 1L, Long::sum);
            buildingNames.putIfAbsent(buildingId, room.getBuilding().getName());
        }

        List<BuildingRoomCountResponse> buildingCounts = byBuilding.entrySet().stream()
                .map(entry -> new BuildingRoomCountResponse(
                        entry.getKey(), buildingNames.get(entry.getKey()), entry.getValue()))
                .toList();
        return new RoomSearchStatisticsResponse(
                rooms.size(),
                sum.divide(BigDecimal.valueOf(rooms.size()), 2, RoundingMode.HALF_UP),
                min, max, byType, buildingCounts);
    }
}

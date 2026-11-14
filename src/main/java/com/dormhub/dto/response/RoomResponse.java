package com.dormhub.dto.response;

import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Integer floor;
    private Long buildingId;
    private String buildingName;
    private String buildingCode;
    private RoomType roomType;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableSlots;
    private BigDecimal pricePerMonth;
    private RoomStatus status;
    private String electricMeterNumber;
    private String waterMeterNumber;
    private String description;
    private Boolean isAvailable; // capacity > currentOccupancy
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

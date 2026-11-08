package com.dormhub.dto.response;

import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRoomResponse {
    private Long roomId;
    private String roomNumber;
    private Integer floor;
    private Long buildingId;
    private String buildingName;
    private String buildingCode;
    private String buildingAddress;
    private Gender buildingGenderRestriction;
    private RoomType roomType;
    private Integer capacity;
    private Integer currentOccupancy;
    private long remainingSlots;
    private BigDecimal pricePerMonth;
    private RoomStatus status;
    private String description;
    private boolean availableForApplication;
    private boolean eligibleForStudent;
}

package com.dormhub.dto.request;

import com.dormhub.entity.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {
    @NotBlank(message = "Room number is required")
    @Size(max = 20)
    private String roomNumber;
    
    @NotNull(message = "Floor is required")
    @Min(1)
    private Integer floor;
    
    @NotNull(message = "Building ID is required")
    private Long buildingId;
    
    @NotNull(message = "Room type is required")
    private RoomType roomType;
    
    @NotNull(message = "Capacity is required")
    @Min(1)
    private Integer capacity;
    
    @NotNull(message = "Price is required")
    @DecimalMin("0.0")
    private BigDecimal pricePerMonth;
    
    private String electricMeterNumber;
    private String waterMeterNumber;
    private String description;
}

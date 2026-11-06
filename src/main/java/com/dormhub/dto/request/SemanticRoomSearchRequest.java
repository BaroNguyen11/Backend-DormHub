package com.dormhub.dto.request;

import com.dormhub.entity.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SemanticRoomSearchRequest {
    @NotBlank
    @Size(max = 500)
    private String query;

    private Long buildingId;
    private RoomType roomType;

    @DecimalMin("0.0")
    private BigDecimal maxPrice;

    @Min(1)
    @Max(50)
    @NotNull
    private Integer limit = 10;
}

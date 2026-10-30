package com.dormhub.dto.request;

import com.dormhub.entity.enums.RoomStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RoomRequest {

    @NotNull(message = "buildingId không được để trống")
    private Long buildingId;

    @NotBlank(message = "roomNumber không được để trống")
    @Size(max = 30, message = "roomNumber tối đa 30 ký tự")
    private String roomNumber;

    @NotNull(message = "floor không được để trống")
    @PositiveOrZero(message = "floor phải lớn hơn hoặc bằng 0")
    private Integer floor;

    @NotNull(message = "capacity không được để trống")
    @Positive(message = "capacity phải lớn hơn 0")
    private Integer capacity;

    @NotBlank(message = "roomType không được để trống")
    @Size(max = 50, message = "roomType tối đa 50 ký tự")
    private String roomType;

    @NotNull(message = "price không được để trống")
    @DecimalMin(value = "0.0", message = "price phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "status không được để trống")
    private RoomStatus status;

    private String description;
}

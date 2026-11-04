package com.dormhub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContractRequest {

    @NotNull(message = "Mã phòng không được để trống")
    @Positive(message = "Mã phòng phải là số dương")
    private Long roomId;

    @NotNull(message = "Mã sinh viên không được để trống")
    @Positive(message = "Mã sinh viên phải là số dương")
    private Long studentId;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    @NotNull(message = "Tiền thuê hàng tháng không được để trống")
    @DecimalMin(value = "0.00", message = "Tiền thuê hàng tháng không được âm")
    @Digits(integer = 38, fraction = 0, message = "Tiền thuê hàng tháng không hợp lệ")
    private BigDecimal monthlyRent;

    @NotNull(message = "Tiền đặt cọc không được để trống")
    @DecimalMin(value = "0.00", message = "Tiền đặt cọc không được âm")
    @Digits(integer = 38, fraction = 0, message = "Tiền đặt cọc không hợp lệ")
    private BigDecimal depositAmount;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
}

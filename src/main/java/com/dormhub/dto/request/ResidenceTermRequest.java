package com.dormhub.dto.request;

import com.dormhub.entity.enums.ResidenceTermStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResidenceTermRequest {

    @NotBlank(message = "Tên đợt đăng ký không được để trống")
    private String name;

    @NotNull(message = "Ngày bắt đầu lưu trú không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc lưu trú không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Ngày mở nhận đơn không được để trống")
    private LocalDate registrationStartDate;

    @NotNull(message = "Ngày đóng nhận đơn không được để trống")
    private LocalDate registrationEndDate;

    private ResidenceTermStatus status;
}
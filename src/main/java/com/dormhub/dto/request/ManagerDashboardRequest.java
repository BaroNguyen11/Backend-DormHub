package com.dormhub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class ManagerDashboardRequest {

    @NotNull
    @Positive
    @Max(999999999)
    private Integer year;
}

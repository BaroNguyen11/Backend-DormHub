package com.dormhub.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInDateRequest {
    @NotNull
    private LocalDate checkInDate;
}

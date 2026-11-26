package com.dormhub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenewalCreateRequest {

    // Legacy clients may still send a complete ISO LocalDateTime. The service validates either form.
    private LocalDateTime requestedEndDate;

    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(12)
    private Integer months;

    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}

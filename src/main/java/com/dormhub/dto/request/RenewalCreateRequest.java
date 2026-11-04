package com.dormhub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenewalCreateRequest {

    @NotNull(message = "Ngày kết thúc mới không được để trống")
    private LocalDateTime requestedEndDate;

    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}

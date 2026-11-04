package com.dormhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutCreateRequest {

    @NotBlank(message = "Lý do trả phòng không được để trống")
    @Size(max = 1000, message = "Lý do trả phòng không được vượt quá 1000 ký tự")
    private String reason;
}

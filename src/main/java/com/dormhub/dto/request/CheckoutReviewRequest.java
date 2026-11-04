package com.dormhub.dto.request;

import com.dormhub.entity.enums.CheckoutRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutReviewRequest {

    @NotNull(message = "Kết quả xử lý không được để trống")
    private CheckoutRequestStatus status;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;
}

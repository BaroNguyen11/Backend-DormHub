package com.dormhub.dto.request;

import com.dormhub.entity.enums.RenewalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenewalReviewRequest {

    @NotNull(message = "Kết quả duyệt không được để trống")
    private RenewalStatus status;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String reviewNote;
}

package com.dormhub.dto.request;

import com.dormhub.entity.enums.DormApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DormApplicationReviewRequest {

    @NotNull(message = "status không được để trống")
    private DormApplicationStatus status;

    @Size(max = 2000, message = "note tối đa 2000 ký tự")
    private String note;

    public boolean requiresNote() {
        return status == DormApplicationStatus.REJECTED;
    }
}

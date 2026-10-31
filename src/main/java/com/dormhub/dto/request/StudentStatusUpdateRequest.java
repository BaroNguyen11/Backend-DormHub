package com.dormhub.dto.request;

import com.dormhub.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private UserStatus status;
}
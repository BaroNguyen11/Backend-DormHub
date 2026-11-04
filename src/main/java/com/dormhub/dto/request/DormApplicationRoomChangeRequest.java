package com.dormhub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DormApplicationRoomChangeRequest {

    @NotNull(message = "roomId không được để trống")
    @Positive(message = "roomId phải lớn hơn 0")
    private Long roomId;
}

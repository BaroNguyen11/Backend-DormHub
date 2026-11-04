package com.dormhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomTransferCreateRequest {

    @Positive(message = "Mã phòng mong muốn phải là số dương")
    private Long preferredRoomId;

    @NotBlank(message = "Lý do chuyển phòng không được để trống")
    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}

package com.dormhub.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomTransferCancelRequest {
    @NotNull
    @AssertTrue(message = "Cần xác nhận sinh viên chưa chuyển phòng thực tế")
    private Boolean studentHasNotMoved;
    @NotBlank(message = "Cần ghi rõ lý do hủy yêu cầu")
    @Size(max = 1000)
    private String note;
}

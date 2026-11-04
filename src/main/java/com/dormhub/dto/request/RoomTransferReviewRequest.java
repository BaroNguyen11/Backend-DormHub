package com.dormhub.dto.request;

import com.dormhub.entity.enums.TransferStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomTransferReviewRequest {

    @NotNull(message = "Kết quả xử lý không được để trống")
    private TransferStatus status;

    @Positive(message = "Mã phòng được duyệt phải là số dương")
    private Long approvedRoomId;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String reviewNote;
}

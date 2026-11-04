package com.dormhub.dto.request;

import com.dormhub.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private MaintenanceStatus status;

    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    private String rejectionReason; // Bắt buộc nhập nếu từ chối
}
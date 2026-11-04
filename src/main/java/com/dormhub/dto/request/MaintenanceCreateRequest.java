package com.dormhub.dto.request;

import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceCreateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 150, message = "Tiêu đề không được vượt quá 150 ký tự")
    private String title;

    @NotBlank(message = "Mô tả chi tiết không được để trống")
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @NotNull(message = "Mức độ ưu tiên không được để trống")
    private MaintenancePriority priority;

    @NotNull(message = "Loại thiết bị hư hỏng không được để trống")
    private MaintenanceType type;

    @Size(max = 500, message = "URL ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    @NotNull(message = "ID phòng không được để trống")
    @Positive(message = "ID phòng phải là số dương")
    private Long roomId;
}
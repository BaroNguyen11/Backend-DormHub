package com.dormhub.dto.response;

import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.MaintenanceType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponse {

    private Long id;
    private String title;
    private String description;
    private MaintenanceStatus status;
    private MaintenancePriority priority;
    private MaintenanceType type;
    private String imageUrl;
    private String rejectionReason;

    private Long studentId;
    private String studentName;
    private String studentCode;
    private String studentPhone;

    private Long roomId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}

package com.dormhub.mapper;

import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.MaintenanceRequest;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {

    public MaintenanceResponse toResponse(MaintenanceRequest entity) {
        return MaintenanceResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getMaintenanceStatus())
                .studentId(entity.getStudent().getId())
                .roomId(entity.getRoom().getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }
}

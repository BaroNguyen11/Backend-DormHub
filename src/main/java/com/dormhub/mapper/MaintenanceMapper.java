
package com.dormhub.mapper;

import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.MaintenanceRequest;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {

    public MaintenanceResponse toResponse(MaintenanceRequest entity) {
        if (entity == null) {
            return null;
        }
        Student student = entity.getStudent();
        User user = (student != null) ? student.getUser() : null;
        return MaintenanceResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getMaintenanceStatus())
                .priority(entity.getPriority())
                .type(entity.getType())
                .imageUrl(entity.getImageUrl())
                .rejectionReason(entity.getRejectionReason())
                .studentId(student != null ? student.getId() : null)
                .studentName(user != null ? user.getFullName() : null)
                .studentCode(student != null ? student.getStudentCode() : null)
                .studentPhone(user != null ? user.getPhone() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }
}

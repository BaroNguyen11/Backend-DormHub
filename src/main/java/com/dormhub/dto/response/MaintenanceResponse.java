
package com.dormhub.dto.response;

import com.dormhub.entity.enums.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private Long studentId;
    private Long roomId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}

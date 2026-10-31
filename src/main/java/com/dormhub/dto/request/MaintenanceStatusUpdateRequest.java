
package com.dormhub.dto.request;

import com.dormhub.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceStatusUpdateRequest {

    @NotNull(message = "Maintenance status is required")
    private MaintenanceStatus status;
}

package com.dormhub.dto.response;

import com.dormhub.entity.enums.ResidenceTermStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidenceTermResponse {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationStartDate;
    private LocalDate registrationEndDate;
    private ResidenceTermStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
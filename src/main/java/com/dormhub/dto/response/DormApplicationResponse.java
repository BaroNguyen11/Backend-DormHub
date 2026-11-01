package com.dormhub.dto.response;

import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

public record DormApplicationResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Gender studentGender,
        Long requestedRoomId,
        String requestedRoomNumber,
        Long requestedBuildingId,
        String requestedBuildingName,
        LocalDateTime requestDate,
        DormApplicationStatus status,
        String note,
        String reviewNote,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long contractId,
        Long depositInvoiceId,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        BigDecimal depositAmount) {
}

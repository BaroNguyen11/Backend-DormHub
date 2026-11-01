package com.dormhub.mapper;

import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class DormApplicationMapper {

    public DormApplicationResponse toResponse(
            DormApplication application,
            Contract contract,
            Invoice depositInvoice) {
        Room room = application.getRequestedRoom();
        return new DormApplicationResponse(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getStudentCode(),
                application.getStudent().getUser().getFullName(),
                application.getStudent().getGender(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomNumber(),
                room == null ? null : room.getBuilding().getId(),
                room == null ? null : room.getBuilding().getBuildingName(),
                application.getRequestDate(),
                application.getStatus(),
                application.getNote(),
                application.getReviewNote(),
                application.getProcessedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                contract == null ? null : contract.getContractId(),
                depositInvoice == null ? null : depositInvoice.getInvoiceId(),
                contract == null ? null : contract.getStartDate(),
                contract == null ? null : contract.getEndDate(),
                contract == null ? null : contract.getDepositAmount());
    }
}

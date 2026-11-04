package com.dormhub.mapper;

import com.dormhub.dto.response.RenewalResponse;
import com.dormhub.entity.RenewalRequest;
import org.springframework.stereotype.Component;

@Component
public class RenewalMapper {

    public RenewalResponse toResponse(RenewalRequest request) {
        if (request == null) {
            return null;
        }

        var contract = request.getContract();
        return RenewalResponse.builder()
                .renewalRequestId(request.getRenewalRequestId())
                .contractId(contract.getContractId())
                .studentId(contract.getStudent().getId())
                .requestedEndDate(request.getRequestedEndDate())
                .contractEndDate(contract.getEndDate())
                .reason(request.getReason())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .reviewedById(request.getReviewedBy() == null ? null : request.getReviewedBy().getId())
                .reviewedAt(request.getReviewedAt())
                .reviewNote(request.getReviewNote())
                .build();
    }
}

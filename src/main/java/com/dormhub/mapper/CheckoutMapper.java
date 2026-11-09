package com.dormhub.mapper;

import com.dormhub.dto.response.CheckoutResponse;
import com.dormhub.entity.CheckoutRequest;
import org.springframework.stereotype.Component;

@Component
public class CheckoutMapper {

    public CheckoutResponse toResponse(CheckoutRequest request) {
        if (request == null) {
            return null;
        }

        return CheckoutResponse.builder()
                .checkoutRequestId(request.getCheckoutRequestId())
                .studentId(request.getStudent().getId())
                .contractId(request.getContract().getContractId())
                .roomId(request.getContract().getRoom().getId())
                .requestDate(request.getRequestDate())
                .checkoutDate(request.getCheckoutDate())
                .status(request.getStatus())
                .reason(request.getReason())
                .processedAt(request.getProcessedAt())
                .note(request.getNote())
                .reviewedByName(request.getReviewedBy() == null
                        ? null : request.getReviewedBy().getFullName())
                .depositStatus(request.getContract().getDepositStatus())
                .build();
    }
}

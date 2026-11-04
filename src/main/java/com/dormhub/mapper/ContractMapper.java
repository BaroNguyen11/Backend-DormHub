package com.dormhub.mapper;

import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.Contract;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(Contract contract) {
        return new ContractResponse(
                contract.getContractId(),
                contract.getRoom() == null ? null : contract.getRoom().getId(),
                contract.getStudent() == null ? null : contract.getStudent().getId(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getDepositAmount(),
                contract.getStatus(),
                contract.getDescription(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }
}

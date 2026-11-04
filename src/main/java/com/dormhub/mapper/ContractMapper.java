package com.dormhub.mapper;

import com.dormhub.dto.request.CreateContractRequest;
import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public Contract toEntity(CreateContractRequest request, Room room, Student student) {
        Contract contract = new Contract();
        contract.setRoom(room);
        contract.setStudent(student);
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyRent(request.getMonthlyRent());
        contract.setDepositAmount(request.getDepositAmount());
        contract.setStatus(ContractStatus.PENDING);
        contract.setDescription(request.getDescription());
        return contract;
    }

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

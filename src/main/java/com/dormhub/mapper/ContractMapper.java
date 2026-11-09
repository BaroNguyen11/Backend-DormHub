package com.dormhub.mapper;

import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Student;
import com.dormhub.entity.Room;
import com.dormhub.entity.Building;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(Contract contract) {
        Student student = contract.getStudent();
        Room room = contract.getRoom();
        
        String studentName = null;
        String studentCode = null;
        if (student != null && student.getUser() != null) {
            studentName = student.getUser().getFullName();
            studentCode = student.getStudentCode();
        }
        
        String roomNumber = null;
        String buildingName = null;
        if (room != null) {
            roomNumber = room.getRoomNumber();
            Building building = room.getBuilding();
            if (building != null) {
                buildingName = building.getName();
            }
        }
        
        // Note: assignmentStatus requires querying RoomAssignment separately
        // since Contract doesn't have a direct relationship to RoomAssignment
        RoomAssignmentStatus assignmentStatus = null;
        
        return new ContractResponse(
                contract.getContractId(),
                room == null ? null : room.getId(),
                student == null ? null : student.getId(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getDepositAmount(),
                contract.getStatus(),
                contract.getDescription(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                studentName,
                studentCode,
                roomNumber,
                buildingName,
                contract.getDepositStatus(),
                assignmentStatus
        );
    }
}

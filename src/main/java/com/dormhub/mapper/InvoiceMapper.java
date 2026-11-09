package com.dormhub.mapper;

import com.dormhub.dto.response.InvoiceDetailResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.dto.response.InvoiceSummaryResponse;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.Building;
import com.dormhub.entity.Student;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceSummaryResponse toSummary(Invoice invoice, boolean overdue) {
        String studentName = extractStudentName(invoice);
        String roomNumber = extractRoomNumber(invoice);
        String buildingName = extractBuildingName(invoice);
        
        return new InvoiceSummaryResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), roomId(invoice), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue, invoice.getTotalAmount(),
                studentName, roomNumber, buildingName);
    }

    public InvoiceResponse toResponse(Invoice invoice, boolean overdue) {
        List<InvoiceDetailResponse> details = invoice.getDetails().stream()
                .map(detail -> new InvoiceDetailResponse(
                        detail.getDetailId(), detail.getItemType(), detail.getDescription(),
                        detail.getQuantity(), detail.getUnitPrice(), detail.getAmount()))
                .toList();
        
        String studentName = extractStudentName(invoice);
        String roomNumber = extractRoomNumber(invoice);
        String buildingName = extractBuildingName(invoice);
        
        return new InvoiceResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), roomId(invoice), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue, invoice.getTotalAmount(), details,
                studentName, roomNumber, buildingName);
    }

    private Long roomId(Invoice invoice) {
        Room room = invoice.getRoom();
        if (room != null) {
            return room.getId();
        }
        Contract contract = invoice.getContract();
        return contract != null && contract.getRoom() != null ? contract.getRoom().getId() : null;
    }

    private Long contractId(Invoice invoice) {
        return invoice.getContract() == null ? null : invoice.getContract().getContractId();
    }
    
    private String extractStudentName(Invoice invoice) {
        Contract contract = invoice.getContract();
        if (contract != null) {
            Student student = contract.getStudent();
            if (student != null && student.getUser() != null) {
                return student.getUser().getFullName();
            }
        }
        return null;
    }
    
    private String extractRoomNumber(Invoice invoice) {
        Room room = invoice.getRoom();
        if (room != null) {
            return room.getRoomNumber();
        }
        Contract contract = invoice.getContract();
        if (contract != null && contract.getRoom() != null) {
            return contract.getRoom().getRoomNumber();
        }
        return null;
    }
    
    private String extractBuildingName(Invoice invoice) {
        Room room = invoice.getRoom();
        if (room != null) {
            Building building = room.getBuilding();
            if (building != null) {
                return building.getName();
            }
        }
        Contract contract = invoice.getContract();
        if (contract != null && contract.getRoom() != null) {
            Building building = contract.getRoom().getBuilding();
            if (building != null) {
                return building.getName();
            }
        }
        return null;
    }
}

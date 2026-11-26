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
@lombok.RequiredArgsConstructor
public class InvoiceMapper {
    private final com.dormhub.repository.RoomAssignmentRepository roomAssignmentRepository;

    private Room historicalRoom(Invoice invoice) {
        if (invoice.getRoom() != null) return invoice.getRoom();
        Contract contract = invoice.getContract();
        if (contract == null) return null;
        if (invoice.getInvoiceType() == com.dormhub.entity.enums.InvoiceType.DEPOSIT) {
            if (contract.getDormApplication() != null) return contract.getDormApplication().getRequestedRoom();
            return roomAssignmentRepository.findFirstByContract_ContractIdOrderByAssignmentIdAsc(contract.getContractId())
                    .map(com.dormhub.entity.RoomAssignment::getRoom)
                    .orElse(contract.getStatus() == com.dormhub.entity.enums.ContractStatus.PENDING ? contract.getRoom() : null);
        }
        if (invoice.getBillingMonth() == null) return null;
        var month = invoice.getBillingMonth();
        var stays = roomAssignmentRepository.findAllocationsForInvoicePeriod(contract.getContractId(),
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        // A room fee is charged once per contract/month; show its first allocation in that period.
        return stays.isEmpty() ? null : stays.get(0).getRoom();
    }

    public InvoiceSummaryResponse toSummary(Invoice invoice, boolean overdue) {
        String studentName = extractStudentName(invoice);
        Room room = historicalRoom(invoice);
        String roomNumber = room == null ? null : room.getRoomNumber();
        String buildingName = room == null || room.getBuilding() == null ? null : room.getBuilding().getName();
        
        return new InvoiceSummaryResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), room == null ? null : room.getId(), contractId(invoice),
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
        Room room = historicalRoom(invoice);
        String roomNumber = room == null ? null : room.getRoomNumber();
        String buildingName = room == null || room.getBuilding() == null ? null : room.getBuilding().getName();
        
        return new InvoiceResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), room == null ? null : room.getId(), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue, invoice.getTotalAmount(), details,
                studentName, roomNumber, buildingName);
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
    
}

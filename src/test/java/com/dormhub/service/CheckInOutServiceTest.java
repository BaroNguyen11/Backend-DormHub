package com.dormhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomAssignmentMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckInOutServiceTest {

    private static final Long CONTRACT_ID = 10L;

    private ContractRepository contractRepository;
    private InvoiceRepository invoiceRepository;
    private RoomAssignmentRepository roomAssignmentRepository;
    private CheckInOutService checkInOutService;

    @BeforeEach
    void setUp() {
        contractRepository = mock(ContractRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        roomAssignmentRepository = mock(RoomAssignmentRepository.class);
        checkInOutService = new CheckInOutService(
                contractRepository,
                invoiceRepository,
                roomAssignmentRepository,
                new RoomAssignmentMapper());
    }

    @Test
    void checkInChangesAssignedRoomAssignmentToCheckedIn() {
        Contract contract = contract(ContractStatus.ACTIVE, LocalDateTime.now().plusDays(30));
        RoomAssignment assignment = assignment(contract, RoomAssignmentStatus.ASSIGNED);
        givenContractAndAssignment(contract, assignment);

        RoomAssignmentResponse response = checkInOutService.checkIn(CONTRACT_ID);

        assertEquals(RoomAssignmentStatus.CHECKED_IN, assignment.getStatus());
        assertNotNull(assignment.getCheckInAt());
        assertEquals(CONTRACT_ID, response.getContractId());
        assertEquals(RoomAssignmentStatus.CHECKED_IN, response.getAssignmentStatus());
        verify(invoiceRepository, never())
                .existsByContract_ContractIdAndStatus(CONTRACT_ID, InvoiceStatus.UNPAID);
    }

    @Test
    void checkInRejectsContractThatIsNotActive() {
        Contract contract = contract(ContractStatus.PENDING, LocalDateTime.now().plusDays(30));
        when(contractRepository.findByContractIdForUpdate(CONTRACT_ID))
                .thenReturn(Optional.of(contract));

        AppException exception = assertThrows(
                AppException.class, () -> checkInOutService.checkIn(CONTRACT_ID));

        assertEquals(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS, exception.getErrorCode());
        verify(roomAssignmentRepository, never())
                .findFirstByContract_ContractIdOrderByAssignmentIdDesc(CONTRACT_ID);
    }

    @Test
    void checkOutRejectsContractWithUnpaidInvoiceWithoutChangingAssignment() {
        Contract contract = contract(ContractStatus.ACTIVE, LocalDateTime.now().plusDays(30));
        RoomAssignment assignment = assignment(contract, RoomAssignmentStatus.CHECKED_IN);
        givenContractAndAssignment(contract, assignment);
        when(invoiceRepository.existsByContract_ContractIdAndStatus(
                CONTRACT_ID, InvoiceStatus.UNPAID)).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> checkInOutService.checkOut(CONTRACT_ID));

        assertEquals(ErrorCode.CONTRACT_HAS_UNPAID_INVOICES, exception.getErrorCode());
        assertEquals(RoomAssignmentStatus.CHECKED_IN, assignment.getStatus());
        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
    }

    @Test
    void checkOutTerminatesContractBeforeItsEndDate() {
        Contract contract = contract(ContractStatus.ACTIVE, LocalDateTime.now().plusDays(30));
        RoomAssignment assignment = assignment(contract, RoomAssignmentStatus.CHECKED_IN);
        givenContractAndAssignment(contract, assignment);
        when(invoiceRepository.existsByContract_ContractIdAndStatus(
                CONTRACT_ID, InvoiceStatus.UNPAID)).thenReturn(false);

        RoomAssignmentResponse response = checkInOutService.checkOut(CONTRACT_ID);

        assertEquals(RoomAssignmentStatus.CHECKED_OUT, assignment.getStatus());
        assertNotNull(assignment.getCheckOutAt());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertEquals(ContractStatus.TERMINATED, response.getContractStatus());
    }

    @Test
    void checkOutExpiresContractAfterItsEndDate() {
        Contract contract = contract(ContractStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        RoomAssignment assignment = assignment(contract, RoomAssignmentStatus.CHECKED_IN);
        givenContractAndAssignment(contract, assignment);
        when(invoiceRepository.existsByContract_ContractIdAndStatus(
                CONTRACT_ID, InvoiceStatus.UNPAID)).thenReturn(false);

        checkInOutService.checkOut(CONTRACT_ID);

        assertEquals(RoomAssignmentStatus.CHECKED_OUT, assignment.getStatus());
        assertEquals(ContractStatus.EXPIRED, contract.getStatus());
    }

    @Test
    void checkOutRejectsAssignmentThatHasNotBeenCheckedIn() {
        Contract contract = contract(ContractStatus.ACTIVE, LocalDateTime.now().plusDays(30));
        RoomAssignment assignment = assignment(contract, RoomAssignmentStatus.ASSIGNED);
        givenContractAndAssignment(contract, assignment);

        AppException exception = assertThrows(
                AppException.class, () -> checkInOutService.checkOut(CONTRACT_ID));

        assertEquals(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS, exception.getErrorCode());
        verify(invoiceRepository, never())
                .existsByContract_ContractIdAndStatus(CONTRACT_ID, InvoiceStatus.UNPAID);
    }

    private void givenContractAndAssignment(Contract contract, RoomAssignment assignment) {
        when(contractRepository.findByContractIdForUpdate(CONTRACT_ID))
                .thenReturn(Optional.of(contract));
        when(roomAssignmentRepository.findFirstByContract_ContractIdOrderByAssignmentIdDesc(
                CONTRACT_ID)).thenReturn(Optional.of(assignment));
    }

    private Contract contract(ContractStatus status, LocalDateTime endDate) {
        Contract contract = new Contract();
        contract.setContractId(CONTRACT_ID);
        contract.setStatus(status);
        contract.setEndDate(endDate);
        return contract;
    }

    private RoomAssignment assignment(Contract contract, RoomAssignmentStatus status) {
        Student student = new Student();
        student.setId(20L);

        Room room = new Room();
        room.setId(30L);

        RoomAssignment assignment = new RoomAssignment();
        assignment.setAssignmentId(40L);
        assignment.setContract(contract);
        assignment.setStudent(student);
        assignment.setRoom(room);
        assignment.setStatus(status);
        return assignment;
    }
}

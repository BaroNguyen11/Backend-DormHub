package com.dormhub.service;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.dto.response.StudentDashboardResponse;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.StudentDashboardRepository;
import com.dormhub.repository.StudentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentDashboardService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final StudentRepository studentRepository;
    private final StudentDashboardRepository dashboardRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomMonthlyChargeCalculator calculator;

    public StudentDashboardService(StudentRepository studentRepository,
                                   StudentDashboardRepository dashboardRepository,
                                   MeterReadingRepository meterReadingRepository,
                                   RoomMonthlyChargeCalculator calculator) {
        this.studentRepository = studentRepository;
        this.dashboardRepository = dashboardRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
    }

    public StudentDashboardResponse getDashboard(User user) {
        if (user == null || user.getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        List<RoomAssignment> residences = dashboardRepository.findResidenceAssignments(student.getId(),
                List.of(RoomAssignmentStatus.CHECKED_IN, RoomAssignmentStatus.CHECKED_OUT));
        RoomAssignment current = residences.stream()
                .filter(assignment -> isCurrent(assignment, now))
                .findFirst().orElse(null);

        StudentDashboardResponse.RoomInfo room = current == null ? null : roomInfo(current.getRoom());
        StudentDashboardResponse.ContractInfo contract = current == null ? null
                : new StudentDashboardResponse.ContractInfo(current.getContract().getContractId(),
                        current.getContract().getStatus());
        StudentDashboardResponse.UtilityUsage utilities = current == null ? null
                : utilityUsage(current.getRoom().getId(), YearMonth.from(now));
        List<StudentDashboardResponse.UnpaidInvoice> invoices = unpaidInvoices(student.getId(), residences, now);
        return new StudentDashboardResponse(user.getFullName(), room, contract, invoices, utilities);
    }

    private boolean isCurrent(RoomAssignment assignment, LocalDateTime now) {
        return assignment.getStatus() == RoomAssignmentStatus.CHECKED_IN
                && !assignment.getCheckInAt().isAfter(now)
                && (assignment.getCheckOutAt() == null || assignment.getCheckOutAt().isAfter(now))
                && assignment.getContract().getStatus() == ContractStatus.ACTIVE
                && !assignment.getContract().getStartDate().isAfter(now)
                && assignment.getContract().getEndDate().isAfter(now);
    }

    private StudentDashboardResponse.RoomInfo roomInfo(Room room) {
        return new StudentDashboardResponse.RoomInfo(room.getId(), room.getRoomNumber(),
                room.getBuilding().getName(), room.getFloor(), room.getCurrentOccupancy(), room.getCapacity());
    }

    private List<StudentDashboardResponse.UnpaidInvoice> unpaidInvoices(
            Long studentId, List<RoomAssignment> residences, LocalDateTime now) {
        List<Invoice> invoices = new ArrayList<>(dashboardRepository.findRoomFeeInvoices(
                studentId, InvoiceType.ROOM_FEE, InvoiceStatus.UNPAID));
        Set<Long> roomIds = residences.stream().map(assignment -> assignment.getRoom().getId())
                .collect(Collectors.toSet());
        if (!roomIds.isEmpty()) {
            dashboardRepository.findUtilityInvoices(roomIds, InvoiceType.UTILITY, InvoiceStatus.UNPAID)
                    .stream()
                    .filter(invoice -> eligibleForUtility(invoice, residences))
                    .forEach(invoices::add);
        }
        return invoices.stream()
                .sorted(Comparator.comparing(Invoice::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Invoice::getInvoiceId))
                .map(invoice -> new StudentDashboardResponse.UnpaidInvoice(
                        invoice.getInvoiceId(), invoice.getInvoiceType(), invoice.getBillingMonth(),
                        invoice.getTotalAmount(), invoice.getDueDate(), invoice.getStatus(),
                        overdue(invoice, now.toLocalDate())))
                .toList();
    }

    private boolean eligibleForUtility(Invoice invoice, List<RoomAssignment> residences) {
        if (invoice.getRoom() == null || invoice.getBillingMonth() == null) {
            return false;
        }
        LocalDateTime monthStart = invoice.getBillingMonth().atDay(1).atStartOfDay();
        LocalDateTime monthEnd = invoice.getBillingMonth().plusMonths(1).atDay(1).atStartOfDay();
        return residences.stream().anyMatch(assignment ->
                assignment.getRoom().getId().equals(invoice.getRoom().getId())
                        && assignment.getCheckInAt().isBefore(monthEnd)
                        && (assignment.getCheckOutAt() == null
                        || assignment.getCheckOutAt().isAfter(monthStart)));
    }

    private boolean overdue(Invoice invoice, LocalDate today) {
        return invoice.getDueDate() != null && invoice.getDueDate().toLocalDate().isBefore(today);
    }

    private StudentDashboardResponse.UtilityUsage utilityUsage(Long roomId, YearMonth currentMonth) {
        List<MeterReading> latest = dashboardRepository.findLatestConfirmedReading(
                roomId, ReadingStatus.CONFIRMED, currentMonth, PageRequest.of(0, 1));
        if (latest.isEmpty()) {
            return null;
        }
        MeterReading reading = latest.getFirst();
        RoomMonthlyChargeResult charge = calculator.calculate(roomId, reading.getReadingMonth(), reading);
        MeterReading previous = meterReadingRepository.findByRoom_IdAndReadingMonth(
                roomId, reading.getReadingMonth().minusMonths(1));
        RoomMonthlyChargeResult previousCharge = previous != null && previous.getStatus() == ReadingStatus.CONFIRMED
                ? calculator.calculate(roomId, previous.getReadingMonth(), previous) : null;
        return new StudentDashboardResponse.UtilityUsage(reading.getReadingMonth(),
                charge.getElectricityConsumption(), charge.getWaterConsumption(),
                previousCharge == null ? null : charge.getElectricityConsumption()
                        .subtract(previousCharge.getElectricityConsumption()),
                previousCharge == null ? null : charge.getWaterConsumption()
                        .subtract(previousCharge.getWaterConsumption()));
    }
}

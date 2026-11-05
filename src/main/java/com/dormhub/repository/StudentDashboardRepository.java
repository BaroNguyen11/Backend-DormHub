package com.dormhub.repository;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StudentDashboardRepository extends Repository<RoomAssignment, Long> {

    @EntityGraph(attributePaths = {"room", "room.building", "contract"})
    @Query("select assignment from RoomAssignment assignment "
            + "where assignment.student.id = :studentId and assignment.checkInAt is not null "
            + "and assignment.status in :statuses "
            + "order by assignment.checkInAt desc, assignment.assignmentId desc")
    List<RoomAssignment> findResidenceAssignments(@Param("studentId") Long studentId,
                                                   @Param("statuses") Collection<RoomAssignmentStatus> statuses);

    @Query("select invoice from Invoice invoice "
            + "where invoice.invoiceType = :type and invoice.status = :status "
            + "and invoice.contract.student.id = :studentId")
    List<Invoice> findRoomFeeInvoices(@Param("studentId") Long studentId,
                                      @Param("type") InvoiceType type,
                                      @Param("status") InvoiceStatus status);

    @EntityGraph(attributePaths = "room")
    @Query("select invoice from Invoice invoice "
            + "where invoice.invoiceType = :type and invoice.status = :status "
            + "and invoice.room.id in :roomIds")
    List<Invoice> findUtilityInvoices(@Param("roomIds") Collection<Long> roomIds,
                                      @Param("type") InvoiceType type,
                                      @Param("status") InvoiceStatus status);

    @Query("select reading from MeterReading reading "
            + "where reading.room.id = :roomId and reading.status = :status "
            + "and reading.readingMonth <= :month order by reading.readingMonth desc")
    List<MeterReading> findLatestConfirmedReading(@Param("roomId") Long roomId,
                                                  @Param("status") ReadingStatus status,
                                                  @Param("month") YearMonth month,
                                                  Pageable pageable);
}

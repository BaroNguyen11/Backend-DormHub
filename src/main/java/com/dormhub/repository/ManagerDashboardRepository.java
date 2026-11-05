package com.dormhub.repository;

import com.dormhub.entity.Room;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.RenewalStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.TransferStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ManagerDashboardRepository extends Repository<Room, Long> {

    long count();

    @Query("select coalesce(sum(room.capacity), 0) from Room room")
    long sumCurrentCapacity();

    @Query("select count(distinct assignment.student.id) from RoomAssignment assignment "
            + "where assignment.status = :status and assignment.checkInAt <= :at "
            + "and assignment.contract.status = :contractStatus "
            + "and assignment.contract.startDate <= :at and assignment.contract.endDate > :at "
            + "and (assignment.checkOutAt is null or assignment.checkOutAt > :at)")
    long countActiveResidents(@Param("status") RoomAssignmentStatus status,
                              @Param("contractStatus") ContractStatus contractStatus,
                              @Param("at") LocalDateTime at);

    @Query("select count(application) from DormApplication application where application.status = :status")
    long countApplications(@Param("status") DormApplicationStatus status);

    @Query("select count(invoice) from Invoice invoice where invoice.status = :status")
    long countInvoices(@Param("status") InvoiceStatus status);

    @Query("select count(request) from RenewalRequest request where request.status = :status")
    long countRenewalRequests(@Param("status") RenewalStatus status);

    @Query("select count(request) from RoomTransferRequest request where request.status = :status")
    long countTransferRequests(@Param("status") TransferStatus status);

    @Query("select count(request) from CheckoutRequest request where request.status = :status")
    long countCheckoutRequests(@Param("status") CheckoutRequestStatus status);

    @Query("select count(request) from MaintenanceRequest request where request.maintenanceStatus = :status")
    long countMaintenanceRequests(@Param("status") MaintenanceStatus status);

    @Query("select count(request) from MaintenanceRequest request "
            + "where request.priority = :priority and request.maintenanceStatus in :statuses")
    long countUrgentMaintenanceRequests(@Param("priority") MaintenancePriority priority,
                                        @Param("statuses") Collection<MaintenanceStatus> statuses);

    @Query("select count(request) from MaintenanceRequest request "
            + "where request.maintenanceStatus = :status "
            + "and request.resolvedAt >= :from and request.resolvedAt < :to")
    long countCompletedMaintenance(@Param("status") MaintenanceStatus status,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("select count(assignment) from RoomAssignment assignment "
            + "where assignment.checkInAt >= :from and assignment.checkInAt < :to")
    long countCheckIns(@Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to);
}

package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import com.dormhub.dto.response.RoommateResponse;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    boolean existsByStudent_IdAndStatusIn(Long studentId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByContract_ContractIdAndStatusIn(
            Long contractId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByRoom_IdAndStatusIn(Long roomId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByRoom_Building_IdAndStatusIn(
            Long buildingId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByRoom_Id(Long roomId);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Page<RoomAssignment> findAllByStudent_Id(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Page<RoomAssignment> findAllByStatus(RoomAssignmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Optional<RoomAssignment> findByAssignmentIdAndStudent_Id(Long assignmentId, Long studentId);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Optional<RoomAssignment> findByAssignmentId(Long assignmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomAssignment> findFirstByContract_ContractIdOrderByAssignmentIdDesc(
            Long contractId);

    @Query("select case when count(assignment) > 0 then true else false end "
            + "from RoomAssignment assignment "
            + "where assignment.room.id = :roomId "
            + "and assignment.student.user.id = :userId "
            + "and assignment.checkInAt is not null "
            + "and assignment.checkInAt < :monthEndExclusive "
            + "and (assignment.checkOutAt is null or assignment.checkOutAt > :monthStart)")
    boolean hasActualOccupancyOverlappingMonth(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEndExclusive") LocalDateTime monthEndExclusive);
    Optional<RoomAssignment> findFirstByStudent_IdAndStatusOrderByAssignmentIdDesc(
            Long studentId, RoomAssignmentStatus status);

    Optional<RoomAssignment> findFirstByStudent_IdAndStatusInOrderByAssignmentIdDesc(
            Long studentId, Collection<RoomAssignmentStatus> statuses);

    @Query("select assignment from RoomAssignment assignment "
            + "where assignment.student.id = :studentId "
            + "and assignment.status in :statuses "
            + "and assignment.contract.status = :contractStatus "
            + "and assignment.contract.startDate <= :now "
            + "and assignment.contract.endDate >= :now "
            + "order by assignment.assignmentId desc")
    List<RoomAssignment> findCurrentResidences(
            @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoomAssignmentStatus> statuses,
            @Param("contractStatus") ContractStatus contractStatus,
            @Param("now") LocalDateTime now);

    @Query("select distinct new com.dormhub.dto.response.RoommateResponse("
            + "student.user.fullName, student.studentCode, student.gender) "
            + "from RoomAssignment assignment join assignment.student student "
            + "where assignment.room.id = :roomId and student.id <> :studentId "
            + "and assignment.status in :statuses "
            + "and assignment.contract.status = :contractStatus "
            + "and assignment.contract.startDate <= :now "
            + "and assignment.contract.endDate >= :now "
            + "order by student.studentCode")
    List<RoommateResponse> findCurrentRoommates(
            @Param("roomId") Long roomId,
            @Param("studentId") Long studentId,
            @Param("statuses") Collection<RoomAssignmentStatus> statuses,
            @Param("contractStatus") ContractStatus contractStatus,
            @Param("now") LocalDateTime now);

    /**
     * Bản có KHÓA biên (PESSIMISTIC_WRITE) của truy vấn phân phòng hiện tại.
     * Dùng cho các luồng check-then-act (tạo yêu cầu chuyển phòng / trả phòng):
     * hai request đồng thời sẽ tuần tự hóa trên dòng phân phòng của sinh viên,
     * nên lần kiểm tra "đã tồn tại request PENDING" phía sau không thể bị vượt qua.
     * Có thể trả về nhiều dòng nếu dữ liệu lịch sử lệch — caller lấy dòng mới nhất.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from RoomAssignment assignment "
            + "where assignment.student.id = :studentId and assignment.status = :status "
            + "order by assignment.assignmentId desc")
    List<RoomAssignment> findAllByStudent_IdAndStatusOrderByAssignmentIdDescForUpdate(
            @Param("studentId") Long studentId, @Param("status") RoomAssignmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from RoomAssignment assignment "
            + "where assignment.assignmentId = :assignmentId")
    Optional<RoomAssignment> findByAssignmentIdForUpdate(
            @Param("assignmentId") Long assignmentId);

    long countByRoom_IdAndStatusIn(Long roomId, Collection<RoomAssignmentStatus> statuses);

    /**
     * Tìm tất cả sinh viên đang ở (ASSIGNED hoặc CHECKED_IN) trong một tòa nhà cụ thể.
     * Dùng để gửi thông báo cho sinh viên theo tòa.
     */
    @Query("select distinct assignment.student from RoomAssignment assignment "
            + "where assignment.room.building.id = :buildingId "
            + "and assignment.status in :statuses")
    List<com.dormhub.entity.Student> findActiveStudentsByBuilding(
            @Param("buildingId") Long buildingId,
            @Param("statuses") Collection<RoomAssignmentStatus> statuses);

    /**
     * Tìm tất cả sinh viên đang có phân phòng hoạt động (ASSIGNED hoặc CHECKED_IN).
     * Dùng để gửi thông báo cho toàn bộ sinh viên.
     */
    @Query("select distinct assignment.student from RoomAssignment assignment "
            + "where assignment.status in :statuses")
    List<com.dormhub.entity.Student> findAllActiveStudents(
            @Param("statuses") Collection<RoomAssignmentStatus> statuses);
}

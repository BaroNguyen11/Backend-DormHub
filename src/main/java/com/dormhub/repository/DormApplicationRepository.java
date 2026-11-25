package com.dormhub.repository;

import com.dormhub.entity.DormApplication;
import com.dormhub.entity.enums.DormApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface DormApplicationRepository extends JpaRepository<DormApplication, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<DormApplication> {

    @EntityGraph(attributePaths = {"student.user", "requestedRoom.building"})
    Page<DormApplication> findAllByStudentId(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student.user", "requestedRoom.building"})
    Page<DormApplication> findAllByStatus(DormApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student.user", "requestedRoom.building"})
    Optional<DormApplication> findByIdAndStudentId(Long id, Long studentId);

    @EntityGraph(attributePaths = {"requestedRoom", "requestedRoom.building"})
    Optional<DormApplication> findFirstByStudent_IdAndStatusOrderByIdDesc(
            Long studentId, DormApplicationStatus status);

    @Override
    @EntityGraph(attributePaths = {"student.user", "requestedRoom.building"})
    Page<DormApplication> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from DormApplication application " +
            "where application.id = :id and application.student.id = :studentId")
    Optional<DormApplication> findByIdAndStudentIdForUpdate(
            @Param("id") Long id,
            @Param("studentId") Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from DormApplication application where application.id = :id")
    Optional<DormApplication> findByIdForUpdate(@Param("id") Long id);

    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<DormApplicationStatus> statuses);

    boolean existsByRequestedRoomId(Long roomId);

    boolean existsByRequestedRoomIdAndStatusIn(
            Long roomId,
            Collection<DormApplicationStatus> statuses);

    long countByRequestedRoomIdAndStatusIn(
            Long roomId,
            Collection<DormApplicationStatus> statuses);

    @Query("select application.requestedRoom.id as roomId, count(application.id) as holdCount "
            + "from DormApplication application where application.requestedRoom.id in :roomIds "
            + "and application.status = :status group by application.requestedRoom.id")
    List<RoomApplicationHoldProjection> findRoomApplicationHoldCounts(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("status") DormApplicationStatus status);

    interface RoomApplicationHoldProjection {
        Long getRoomId();

        Long getHoldCount();
    }

    boolean existsByRequestedRoomBuildingIdAndStatusIn(
            Long buildingId,
            Collection<DormApplicationStatus> statuses);

    long countByStatus(DormApplicationStatus status);

}

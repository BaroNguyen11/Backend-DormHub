package com.dormhub.repository;

import com.dormhub.entity.Notification;
import com.dormhub.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where "
            + "(n.type = :managerType or (n.type = :legacyType and n.refId is null)) "
            + "and (:search = '' or lower(n.title) like lower(concat('%', :search, '%')) "
            + "or lower(n.body) like lower(concat('%', :search, '%'))) "
            + "and (:target = '' or (:target = 'BUILDING' and n.type = :managerType and n.refId is not null) "
            + "or (:target = 'ALL' and n.type = :managerType and n.refId is null) "
            + "or (:target = 'LEGACY' and n.type = :legacyType))")
    Page<Notification> findManagerHistory(@Param("managerType") NotificationType managerType,
            @Param("legacyType") NotificationType legacyType, @Param("search") String search,
            @Param("target") String target, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    /** Chống gửi trùng: đã từng bắn thông báo cùng loại cho cùng đối tượng (refId) chưa? */
    boolean existsByTypeAndRefId(NotificationType type, Long refId);
}

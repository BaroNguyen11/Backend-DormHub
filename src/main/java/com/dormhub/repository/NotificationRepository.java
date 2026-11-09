package com.dormhub.repository;

import com.dormhub.entity.Notification;
import com.dormhub.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    /** Chống gửi trùng: đã từng bắn thông báo cùng loại cho cùng đối tượng (refId) chưa? */
    boolean existsByTypeAndRefId(NotificationType type, Long refId);
}
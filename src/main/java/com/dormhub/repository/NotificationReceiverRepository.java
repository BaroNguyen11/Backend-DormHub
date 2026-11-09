package com.dormhub.repository;

import com.dormhub.entity.NotificationReceiver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, Long> {

    // Danh sách tất cả thông báo của User (fetch kèm notification để chống N+1)
    @EntityGraph(attributePaths = {"notification"})
    Page<NotificationReceiver> findAllByUser_IdOrderByNotification_CreatedAtDesc(Long userId, Pageable pageable);

    // Danh sách chỉ thông báo chưa đọc của User
    @EntityGraph(attributePaths = {"notification"})
    Page<NotificationReceiver> findAllByUser_IdAndIsReadFalseOrderByNotification_CreatedAtDesc(Long userId, Pageable pageable);

    // Đếm số lượng thông báo chưa đọc (dùng cho badge đỏ icon quả chuông)
    long countByUser_IdAndIsReadFalse(Long userId);

    // Lấy 1 thông báo thuộc quyền sở hữu của user
    @EntityGraph(attributePaths = {"notification"})
    Optional<NotificationReceiver> findByIdAndUser_Id(Long id, Long userId);

    // Đánh dấu tất cả thông báo của user là đã đọc
    @Modifying
    @Query("UPDATE NotificationReceiver nr SET nr.isRead = true, nr.readAt = :now WHERE nr.user.id = :userId AND nr.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Dọn dẹp: xóa bản ghi nhận của các thông báo cũ hơn mốc thời gian (chạy trước khi xóa Notification cha)
    @Modifying
    @Query("DELETE FROM NotificationReceiver nr WHERE nr.notification.createdAt < :cutoff")
    int deleteByNotification_CreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
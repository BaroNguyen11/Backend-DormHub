package com.dormhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_receivers",
        indexes = {
                @Index(name = "idx_noti_receiver_user_read", columnList = "user_id, is_read")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationReceiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "notification_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_noti_receivers_notification")
    )
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_noti_receivers_user")
    )
    private User user;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
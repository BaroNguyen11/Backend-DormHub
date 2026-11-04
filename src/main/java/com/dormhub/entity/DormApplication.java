package com.dormhub.entity;

import com.dormhub.entity.enums.DormApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dorm_applications", indexes = {
        @Index(name = "idx_dorm_applications_student", columnList = "student_id"),
        @Index(name = "idx_dorm_applications_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class DormApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_dorm_applications_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "requested_room_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_dorm_applications_requested_room"))
    private Room requestedRoom;

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DormApplicationStatus status = DormApplicationStatus.PENDING;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        requestDate = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

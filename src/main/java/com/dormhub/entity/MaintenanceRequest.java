package com.dormhub.entity;

import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.MaintenanceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_status", nullable = false, length = 20)
    private MaintenanceStatus maintenanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenancePriority priority; // Mức độ ưu tiên

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MaintenanceType type; // Loại thiết bị hỏng

    @Column(name = "image_url", length = 500)
    private String imageUrl; // Ảnh chụp minh chứng sự cố

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason; // Lý do từ chối nếu status là REJECTED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "student_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_maintenance_requests_student")
    )
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_maintenance_requests_room")
    )
    private Room room;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
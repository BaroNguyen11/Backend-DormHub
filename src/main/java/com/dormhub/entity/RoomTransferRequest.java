package com.dormhub.entity;

import com.dormhub.entity.enums.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_transfer_requests", indexes = {
        @Index(name = "idx_room_transfer_requests_student_status", columnList = "student_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_transfer_requests_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_assignment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_transfer_requests_assignment"))
    private RoomAssignment currentAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_room_id",
            foreignKey = @ForeignKey(name = "fk_room_transfer_requests_preferred_room"))
    private Room preferredRoom;

    @Column(length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_room_id",
            foreignKey = @ForeignKey(name = "fk_room_transfer_requests_approved_room"))
    private Room approvedRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by",
            foreignKey = @ForeignKey(name = "fk_room_transfer_requests_reviewer"))
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}

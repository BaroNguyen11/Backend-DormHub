package com.dormhub.entity;

import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "rooms",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_rooms_building_room", 
        columnNames = {"building_id", "room_number"}
    ),
    indexes = {
        @Index(name = "idx_rooms_building", columnList = "building_id"),
        @Index(name = "idx_rooms_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;
    
    @Column(nullable = false)
    private Integer floor;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;
    
    @Column(nullable = false)
    private Integer capacity;
    
    @Column(name = "current_occupancy", nullable = false)
    @Builder.Default
    private Integer currentOccupancy = 0;
    
    @Column(name = "price_per_month", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerMonth;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;
    
    @Column(name = "electric_meter_number", length = 50)
    private String electricMeterNumber;
    
    @Column(name = "water_meter_number", length = 50)
    private String waterMeterNumber;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
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

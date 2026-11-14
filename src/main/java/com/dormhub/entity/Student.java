/*
 * @ (#) Student .java     1.0     10/27/2026
 * Copyright (c) 2026 IUH.All Rights Reserved.
 */
package com.dormhub.entity;

import com.dormhub.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/*
 *@description: this is a class to test the Student class
 *@author: Bao Nguyen
 *@Date: 10/27/2026
 *@version:      1.0
 */
@Setter
@Getter
@Entity
@Table(name = "students",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_students_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_students_code", columnNames = "student_code"),
                @UniqueConstraint(name = "uk_students_cic", columnNames = "cic")
        }

)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_code", unique = true, length = 8)
    private String studentCode;

    @Column(name = "full_name", length = 50)
    private String fullName;

    @Column(length = 50)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 255)
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;


    @Column(name = "cic", nullable = true)
    private String cic;


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, foreignKey = @ForeignKey(name = "fk_students_user"))
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    // Current room cache synchronized from assignment and contract workflows.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(name = "fk_students_current_room"))
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

package com.dormhub.repository;


import com.dormhub.entity.Student;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.user.id = :userId")
    Optional<Student> findByUserIdForUpdate(@Param("userId") Long userId);

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);
}

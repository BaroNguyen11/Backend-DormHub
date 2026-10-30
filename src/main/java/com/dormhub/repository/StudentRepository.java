package com.dormhub.repository;


import com.dormhub.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Student> findAll(Specification<Student> spec, Pageable pageable);
}
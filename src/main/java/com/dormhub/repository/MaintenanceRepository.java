
package com.dormhub.repository;

import com.dormhub.entity.MaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long>, JpaSpecificationExecutor<MaintenanceRequest> {

    List<MaintenanceRequest> findAllByStudent_IdOrderByCreatedAtDesc(Long studentId);
    @EntityGraph(attributePaths = {"student", "student.user", "room"})
    Page<MaintenanceRequest> findAllByStudent_Id(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "room"})
    Optional<MaintenanceRequest> findByIdAndStudent_Id(Long id, Long studentId);

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "room"})
    Page<MaintenanceRequest> findAll(Specification<MaintenanceRequest> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "room"})
    Optional<MaintenanceRequest> findById(Long id);
}

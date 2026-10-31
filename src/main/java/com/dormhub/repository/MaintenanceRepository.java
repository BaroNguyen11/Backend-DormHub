
package com.dormhub.repository;

import com.dormhub.entity.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findAllByStudent_IdOrderByCreatedAtDesc(Long studentId);

    Optional<MaintenanceRequest> findByIdAndStudent_Id(Long id, Long studentId);
}

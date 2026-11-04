package com.dormhub.repository;

import com.dormhub.entity.ResidenceTerm;
import com.dormhub.entity.enums.ResidenceTermStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResidenceTermRepository extends JpaRepository<ResidenceTerm, Long> {

    // Tìm đợt đăng ký đang OPEN
    Optional<ResidenceTerm> findFirstByStatus(ResidenceTermStatus status);

    boolean existsByStatusAndIdNot(ResidenceTermStatus status, Long id);
}
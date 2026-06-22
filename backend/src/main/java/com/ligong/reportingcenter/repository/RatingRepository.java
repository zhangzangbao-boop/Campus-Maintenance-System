package com.ligong.reportingcenter.repository;

import com.ligong.reportingcenter.domain.entity.Rating;
import com.ligong.reportingcenter.domain.entity.RepairTicket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByTicket(RepairTicket ticket);
    boolean existsByTicket(RepairTicket ticket);
}


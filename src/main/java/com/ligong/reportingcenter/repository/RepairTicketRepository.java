package com.ligong.reportingcenter.repository;

import com.ligong.reportingcenter.domain.entity.Category;
import com.ligong.reportingcenter.domain.entity.RepairTicket;
import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.enums.TicketStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairTicketRepository extends JpaRepository<RepairTicket, Long> {
    List<RepairTicket> findByStudent(User student);
    List<RepairTicket> findByStaff(User staff);
    List<RepairTicket> findByStatus(TicketStatus status);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE rt.category.categoryId = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE rt.status = :status")
    Long countByStatus(@Param("status") TicketStatus status);
}
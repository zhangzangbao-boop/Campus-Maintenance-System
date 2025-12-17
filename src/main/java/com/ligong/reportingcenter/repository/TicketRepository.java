package com.ligong.reportingcenter.repository;

import com.ligong.reportingcenter.domain.entity.Category;
import com.ligong.reportingcenter.domain.entity.RepairTicket;
import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.entity.Rating;
import com.ligong.reportingcenter.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<RepairTicket, Long> {
    List<RepairTicket> findByStudent(User student);
    List<RepairTicket> findByStaff(User staff);
    List<RepairTicket> findByStatus(TicketStatus status);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE rt.category.categoryId = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE FUNCTION('DATE_FORMAT', rt.createdAt, '%Y-%m') = :monthPattern")
    Long countByMonth(@Param("monthPattern") String monthPattern);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE rt.status = :status")
    Long countByStatus(@Param("status") TicketStatus status);
    
    // 新增方法：获取位置报修数量统计
    @Query("SELECT t.locationText, COUNT(t.id) FROM RepairTicket t GROUP BY t.locationText ORDER BY COUNT(t.id) DESC")
    List<Object[]> findLocationStats();

    // 新增方法：获取维修人员评分统计
    @Query("SELECT u.userId, u.nickname, AVG(r.score), COUNT(t.id) " +
           "FROM RepairTicket t " +
           "JOIN t.staff u " +
           "LEFT JOIN Rating r ON t.id = r.ticket.id " +
           "WHERE r.score IS NOT NULL " +
           "GROUP BY u.id, u.nickname " +
           "ORDER BY AVG(r.score) DESC")
    List<Object[]> findRepairmanRatingStats();
}

package com.ligong.reportingcenter.repository;

import com.ligong.reportingcenter.domain.entity.Category;
import com.ligong.reportingcenter.domain.entity.RepairTicket;
import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.entity.Rating;
import com.ligong.reportingcenter.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<RepairTicket, Long> {
    // 查询未删除的工单
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.student = :student AND (rt.deleted IS NULL OR rt.deleted = false)")
    List<RepairTicket> findByStudent(@Param("student") User student);
    
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.staff = :staff AND (rt.deleted IS NULL OR rt.deleted = false)")
    List<RepairTicket> findByStaff(@Param("staff") User staff);
    
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.status = :status AND (rt.deleted IS NULL OR rt.deleted = false)")
    List<RepairTicket> findByStatus(@Param("status") TicketStatus status);
    
    // 查询所有工单（包括已删除的，供管理员使用）
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.student = :student")
    List<RepairTicket> findByStudentIncludeDeleted(@Param("student") User student);
    
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.staff = :staff")
    List<RepairTicket> findByStaffIncludeDeleted(@Param("staff") User staff);
    
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.status = :status")
    List<RepairTicket> findByStatusIncludeDeleted(@Param("status") TicketStatus status);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE rt.category.categoryId = :categoryId AND (rt.deleted IS NULL OR rt.deleted = false)")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE FUNCTION('DATE_FORMAT', rt.createdAt, '%Y-%m') = :monthPattern AND (rt.deleted IS NULL OR rt.deleted = false)")
    Long countByMonth(@Param("monthPattern") String monthPattern);
    
    @Query("SELECT COUNT(rt) FROM RepairTicket rt WHERE rt.status = :status AND (rt.deleted IS NULL OR rt.deleted = false)")
    Long countByStatus(@Param("status") TicketStatus status);
    
    // 新增方法：获取位置报修数量统计（排除已删除）
    @Query("SELECT t.locationText, COUNT(t.id) FROM RepairTicket t WHERE (t.deleted IS NULL OR t.deleted = false) GROUP BY t.locationText ORDER BY COUNT(t.id) DESC")
    List<Object[]> findLocationStats();

    // 获取维修人员评分统计（基于 repairman_stats 汇总表）
    @Query(
        value = "SELECT " +
                "  s.repairman_id, " +
                "  u.name AS repairman_name, " +
                "  s.avg_rating, " +
                "  s.completed_tickets " +
                "FROM repairman_stats s " +
                "JOIN sys_user u ON u.user_number = s.repairman_id " +
                "WHERE u.role = 'STAFF' " +
                "ORDER BY s.avg_rating DESC",
        nativeQuery = true
    )
    List<Object[]> findRepairmanRatingStats();

    // 获取按类别的统计信息（基于 category_stats 汇总表）
    @Query(
        value = "SELECT " +
                "  s.category_key, " +
                "  s.category_key AS category_name, " +
                "  s.total_tickets, " +
                "  s.completed_tickets, " +
                "  s.rated_tickets, " +
                "  s.avg_rating " +
                "FROM category_stats s",
        nativeQuery = true
    )
    List<Object[]> findCategoryStatsFromView();

    // 使用悲观锁查询工单（用于关键操作）
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RepairTicket rt WHERE rt.ticketId = :id")
    Optional<RepairTicket> findByIdWithLock(@Param("id") Long id);
}

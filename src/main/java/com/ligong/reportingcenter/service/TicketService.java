package com.ligong.reportingcenter.service;

import com.ligong.reportingcenter.domain.entity.Category;
import com.ligong.reportingcenter.domain.entity.Rating;
import com.ligong.reportingcenter.domain.entity.RepairTicket;
import com.ligong.reportingcenter.domain.entity.TicketImage;
import com.ligong.reportingcenter.domain.entity.TicketStatusLog;
import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.enums.TicketStatus;
import com.ligong.reportingcenter.domain.enums.UserRole;
import com.ligong.reportingcenter.dto.*;
import com.ligong.reportingcenter.dto.request.TicketAssignRequest;
import com.ligong.reportingcenter.dto.request.TicketCreateRequest;
import com.ligong.reportingcenter.dto.request.TicketImageRequest;
import com.ligong.reportingcenter.dto.request.TicketRatingRequest;
import com.ligong.reportingcenter.dto.request.TicketStatusUpdateRequest;
import com.ligong.reportingcenter.exception.BusinessException;
import com.ligong.reportingcenter.repository.RatingRepository;
import com.ligong.reportingcenter.repository.TicketImageRepository;
import com.ligong.reportingcenter.repository.TicketRepository;
import com.ligong.reportingcenter.repository.TicketStatusLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketImageRepository imageRepository;
    private final TicketStatusLogRepository statusLogRepository;
    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    @Transactional
    public TicketDetailDto createTicket(TicketCreateRequest request) {
        User student = userService.loadActiveUser(request.studentId());
        if (student.getRole() != UserRole.STUDENT) {
            throw new BusinessException("仅学生账号可以提交报修单");
        }

        // 验证分类是否存在
        Category category = categoryService.getById(request.categoryId());
        if (category == null) {
            throw new BusinessException("指定的分类不存在");
        }

        RepairTicket ticket = new RepairTicket();
        ticket.setStudent(student);
        ticket.setCategory(category);
        ticket.setLocationText(request.locationText());
        ticket.setDescription(request.description());
        ticket.setStatus(TicketStatus.WAITING_ACCEPT);
        ticket.setCreatedAt(LocalDateTime.now());  // 明确设置
        ticket.setPriority(request.priority());

        // 关键修改：确保保存后获取到含有ID的实体
        ticket = ticketRepository.save(ticket);

        // 强制刷新确保数据写入数据库
        ticketRepository.flush();

        appendStatusLog(ticket, null, TicketStatus.WAITING_ACCEPT, student);

        if (request.imageUrls() != null) {
            for (String url : request.imageUrls()) {
                TicketImage image = new TicketImage();
                image.setTicket(ticket);
                image.setImageUrl(url);
                image.setUploadedAt(LocalDateTime.now()); // 明确设置时间
                imageRepository.save(image);
            }
        }
        ticketRepository.flush();
        return toDetailDto(ticket);
    }


    @Transactional
    public TicketDetailDto assignTicket(Long ticketId, TicketAssignRequest request) {
        RepairTicket ticket = findTicket(ticketId);
        User operator = userService.loadActiveUser(request.operatorId());
        User staff = userService.loadActiveUser(request.staffId());
        if (staff.getRole() != UserRole.STAFF) {
            throw new BusinessException("仅维修工角色可以被分配");
        }
        ticket.setStaff(staff);
        ticket.setAssignedAt(LocalDateTime.now());
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        appendStatusLog(ticket, oldStatus, TicketStatus.IN_PROGRESS, operator);
        return toDetailDto(ticket);
    }

    @Transactional
    public TicketDetailDto updateStatus(Long ticketId, TicketStatusUpdateRequest request) {
        RepairTicket ticket = findTicket(ticketId);
        User operator = userService.loadActiveUser(request.operatorId());
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.newStatus();

        // 新增状态转换合法性校验
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new BusinessException("不允许从 " + oldStatus + " 转换到 " + newStatus);
        }

        // 状态变更逻辑（补充完整字段映射）
        if (newStatus == TicketStatus.REJECTED) {
            if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                throw new BusinessException("驳回时必须填写理由");
            }
            ticket.setRejectionReason(request.rejectionReason());
            ticket.setStaff(null);
            ticket.setAssignedAt(null);
            ticket.setCompletedAt(null);
            ticket.setClosedAt(LocalDateTime.now());
        } else if (newStatus == TicketStatus.RESOLVED) {
            ticket.setCompletedAt(LocalDateTime.now());
        } else if (newStatus == TicketStatus.WAITING_FEEDBACK) {
            if (ticket.getCompletedAt() == null) {
                ticket.setCompletedAt(LocalDateTime.now());
            }
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticket.setStatus(newStatus);
        appendStatusLog(ticket, oldStatus, newStatus, operator);
        return toDetailDto(ticket);
    }

    // 新增状态转换规则校验方法
    private boolean isValidStatusTransition(TicketStatus oldStatus, TicketStatus newStatus) {
        return switch (oldStatus) {
            case WAITING_ACCEPT ->
                    newStatus == TicketStatus.IN_PROGRESS || newStatus == TicketStatus.REJECTED;
            case IN_PROGRESS ->
                    newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.REJECTED;
            case RESOLVED ->
                    newStatus == TicketStatus.WAITING_FEEDBACK || newStatus == TicketStatus.CLOSED;
            case WAITING_FEEDBACK ->
                    newStatus == TicketStatus.FEEDBACKED || newStatus == TicketStatus.CLOSED;
            case FEEDBACKED ->
                    newStatus == TicketStatus.CLOSED;
            case REJECTED, CLOSED ->
                    false; // 已驳回/关闭状态不允许转换
        };
    }


    @Transactional
    public TicketDetailDto addImage(Long ticketId, TicketImageRequest request) {
        RepairTicket ticket = findTicket(ticketId);
        TicketImage image = new TicketImage();
        image.setTicket(ticket);
        image.setImageUrl(request.imageUrl());
        imageRepository.save(image);
        return toDetailDto(ticket);
    }

    @Transactional
    public TicketDetailDto rateTicket(Long ticketId, TicketRatingRequest request) {
        RepairTicket ticket = findTicket(ticketId);
        if (ticket.getStatus() != TicketStatus.WAITING_FEEDBACK) {
            throw new BusinessException("当前状态不可评价");
        }
        if (!Objects.equals(ticket.getStudent().getUserId(), request.studentId())) {
            throw new BusinessException("仅提交该报修单的学生可以评价");
        }
        if (ratingRepository.existsByTicket(ticket)) {
            throw new BusinessException("该报修单已评价");
        }
        Rating rating = new Rating();
        rating.setTicket(ticket);
        rating.setStudent(ticket.getStudent());
        rating.setStaff(ticket.getStaff());
        rating.setScore(request.score());
        rating.setComment(request.comment());
        ratingRepository.save(rating);

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.FEEDBACKED);
        ticket.setClosedAt(LocalDateTime.now());
        appendStatusLog(ticket, oldStatus, TicketStatus.FEEDBACKED, ticket.getStudent());
        return toDetailDto(ticket);
    }

    // 新增方法：更新维修备注
    @Transactional
    public TicketDetailDto updateRepairNotes(Long ticketId, String repairNotes, String operatorId) {
        RepairTicket ticket = findTicket(ticketId);
        User operator = userService.loadActiveUser(operatorId);
        
        ticket.setRepairNotes(repairNotes);
        ticketRepository.save(ticket);
        
        // 记录日志
        appendStatusLog(ticket, ticket.getStatus(), ticket.getStatus(), operator);
        return toDetailDto(ticket);
    }

    // 新增方法：更新处理备注
    @Transactional
    public TicketDetailDto updateProcessNotes(Long ticketId, String processNotes, String operatorId) {
        RepairTicket ticket = findTicket(ticketId);
        User operator = userService.loadActiveUser(operatorId);
        
        ticket.setProcessNotes(processNotes);
        ticketRepository.save(ticket);
        
        // 记录日志
        appendStatusLog(ticket, ticket.getStatus(), ticket.getStatus(), operator);
        return toDetailDto(ticket);
    }

    // 新增方法：设置预计完成时间
    @Transactional
    public TicketDetailDto setEstimatedCompletionTime(Long ticketId, LocalDateTime estimatedTime, String operatorId) {
        RepairTicket ticket = findTicket(ticketId);
        User operator = userService.loadActiveUser(operatorId);
        
        ticket.setEstimatedCompletionTime(estimatedTime);
        ticketRepository.save(ticket);
        
        // 记录日志
        appendStatusLog(ticket, ticket.getStatus(), ticket.getStatus(), operator);
        return toDetailDto(ticket);
    }

    @Transactional
    public TicketDetailDto getTicketDetail(Long ticketId) {
        RepairTicket ticket = findTicket(ticketId);
        return toDetailDto(ticket);
    }

    @Transactional
    public List<TicketSummaryDto> listByStudent(String studentId) {
        User student = userService.loadActiveUser(studentId);
        return ticketRepository.findByStudent(student)
            .stream()
            .sorted(Comparator.comparing(RepairTicket::getCreatedAt).reversed())
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<TicketSummaryDto> listByStaff(String staffId) {
        User staff = userService.loadActiveUser(staffId);
        return ticketRepository.findByStaff(staff)
            .stream()
            .sorted(Comparator.comparing(RepairTicket::getCreatedAt).reversed())
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<TicketSummaryDto> listByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status)
            .stream()
            .sorted(Comparator.comparing(RepairTicket::getCreatedAt).reversed())
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<TicketSummaryDto> listAll() {
        return ticketRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(RepairTicket::getCreatedAt).reversed())
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Long countByCategory(Long categoryId) {
        return ticketRepository.countByCategoryId(categoryId);
    }
    
    @Transactional(readOnly = true)
    public Long countByStatus(TicketStatus status) {
        return ticketRepository.countByStatus(status);
    }

    // 新增方法：获取位置报修数量排行
    @Transactional(readOnly = true)
    public List<LocationStatsDto> getLocationStats() {
        List<Object[]> results = ticketRepository.findLocationStats();
        return results.stream()
            .map(row -> new LocationStatsDto(
                (String) row[0],
                ((Long) row[1]).intValue()
            ))
            // 限制返回前10条记录
            .limit(10)
            .collect(Collectors.toList());
    }

    // 新增方法：获取维修人员评分排行
    @Transactional(readOnly = true)
    public List<RepairmanRatingStatsDto> getRepairmanRatingStats() {
        List<Object[]> results = ticketRepository.findRepairmanRatingStats();
        return results.stream()
            .map(row -> new RepairmanRatingStatsDto(
                (String) row[0],
                (String) row[1],
                ((Double) row[2]).intValue(),  // 平均分
                ((Long) row[3]).intValue()      // 报修单数
            ))
            // 限制返回前10条记录
            .limit(10)
            .collect(Collectors.toList());
    }

    // 新增方法：获取月度统计数据
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyStats() {
        // 获取最近12个月的统计数据
        LocalDate now = LocalDate.now();
        Map<String, Object> monthlyStats = new HashMap<>();
        
        // 初始化月度数据
        List<Map<String, Object>> monthsData = new ArrayList<>();
        
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStr);
            monthData.put("count", ticketRepository.countByMonth(monthStr));
            monthsData.add(monthData);
        }
        
        monthlyStats.put("months", monthsData);
        
        // 统计各类状态的数量
        Map<String, Long> statusStats = new HashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            statusStats.put(status.name(), ticketRepository.countByStatus(status));
        }
        monthlyStats.put("statusDistribution", statusStats);
        
        return monthlyStats;
    }

    private RepairTicket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "报修单不存在"));
    }

    private void appendStatusLog(RepairTicket ticket, TicketStatus oldStatus, TicketStatus newStatus, User operator) {
        TicketStatusLog log = new TicketStatusLog();
        log.setTicket(ticket);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangedBy(operator);
        log.setLogTime(LocalDateTime.now());
        statusLogRepository.save(log);
    }

    private TicketDetailDto toDetailDto(RepairTicket ticket) {
        List<TicketImageDto> images = imageRepository.findByTicket(ticket)
            .stream()
            .map(image -> new TicketImageDto(image.getImageId(), image.getImageUrl(), image.getUploadedAt()))
            .collect(Collectors.toList());
        List<TicketStatusLogDto> logs = statusLogRepository.findByTicketOrderByLogTimeAsc(ticket)
            .stream()
            .map(log -> new TicketStatusLogDto(
                log.getLogId(),
                log.getOldStatus(),
                log.getNewStatus(),
                log.getChangedBy() != null ? log.getChangedBy().getUserId() : null,
                log.getLogTime()
            ))
            .collect(Collectors.toList());
        RatingDto ratingDto = ratingRepository.findByTicket(ticket)
            .map(rating -> new RatingDto(
                rating.getRatingId(),
                rating.getScore(),
                rating.getComment(),
                rating.getStudent() != null ? rating.getStudent().getUserId() : null,
                rating.getStaff() != null ? rating.getStaff().getUserId() : null,
                rating.getRatedAt()
            ))
            .orElse(null);

        return new TicketDetailDto(
            ticket.getTicketId(),
            ticket.getStatus(),
            ticket.getCategory() != null ? ticket.getCategory().getCategoryName() : null,
            ticket.getStudent() != null ? ticket.getStudent().getUserId() : null,
            ticket.getStudent() != null ? ticket.getStudent().getNickname() : null,
            ticket.getStaff() != null ? ticket.getStaff().getUserId() : null,
            ticket.getStaff() != null ? ticket.getStaff().getNickname() : null,
            ticket.getLocationText(),
            ticket.getDescription(),
            ticket.getRejectionReason(),
            ticket.getPriority(),
            ticket.getRepairNotes(),
            ticket.getProcessNotes(),
            ticket.getEstimatedCompletionTime(),
            ticket.getCreatedAt(),
            ticket.getAssignedAt(),
            ticket.getCompletedAt(),
            ticket.getClosedAt(),
            images,
            logs,
            ratingDto
        );
    }

    private TicketSummaryDto toSummaryDto(RepairTicket ticket) {
        return new TicketSummaryDto(
            ticket.getTicketId(),
            ticket.getStatus(),
            ticket.getCategory() != null ? ticket.getCategory().getCategoryName() : null,
            ticket.getStudent() != null ? ticket.getStudent().getUserId() : null,
            ticket.getStaff() != null ? ticket.getStaff().getUserId() : null,
            ticket.getLocationText(),
            ticket.getPriority(),
            ticket.getCreatedAt()
        );
    }
}
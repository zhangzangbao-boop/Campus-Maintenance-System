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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketImageRepository imageRepository;
    private final TicketStatusLogRepository statusLogRepository;
    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

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
    public TicketDetailDto createTicket(TicketCreateRequest request, List<MultipartFile> images) {
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

        // 处理上传的图片文件，保存到文件系统
        if (images != null && !images.isEmpty()) {
            try {
                // 创建上传目录
                Path uploadDir = Paths.get(uploadPath);
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        // 生成唯一文件名
                        String originalFilename = image.getOriginalFilename();
                        String extension = "";
                        if (originalFilename != null && originalFilename.contains(".")) {
                            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        }
                        String filename = UUID.randomUUID().toString() + extension;

                        // 保存文件到文件系统
                        Path filePath = uploadDir.resolve(filename);
                        Files.write(filePath, image.getBytes());

                        // 保存图片URL到数据库
                        TicketImage ticketImage = new TicketImage();
                        ticketImage.setTicket(ticket);
                        ticketImage.setImageUrl("/uploads/" + filename);
                        ticketImage.setUploadedAt(LocalDateTime.now());
                        imageRepository.save(ticketImage);
                    }
                }
            } catch (IOException e) {
                throw new BusinessException("图片保存失败: " + e.getMessage());
            }
        }

        ticketRepository.flush();
        return toDetailDto(ticket);
    }

    @Transactional
    public TicketDetailDto createTicketFromFormData(
            String studentId,
            Long categoryId,
            String locationText,
            String description,
            String priority,
            List<MultipartFile> images) {
        // 构造请求对象
        TicketCreateRequest request = new TicketCreateRequest(
            studentId,
            categoryId,
            locationText,
            description,
            priority,
            Collections.emptyList() // 图片URL将在后续处理
        );
        
        return createTicket(request, images);
    }

    @Transactional
    public TicketDetailDto assignTicket(Long ticketId, TicketAssignRequest request) {
        try {
            // 使用悲观锁防止并发分配
            RepairTicket ticket = findTicketWithLock(ticketId);
            User operator = userService.loadActiveUser(request.operatorId());
            User staff = userService.loadActiveUser(request.staffId());
            if (staff.getRole() != UserRole.STAFF) {
                throw new BusinessException("仅维修工角色可以被分配");
            }
            // 检查工单是否已被分配给其他维修工
            if (ticket.getStaff() != null 
                    && !ticket.getStaff().getUserId().equals(request.staffId())
                    && ticket.getStatus() != TicketStatus.WAITING_ACCEPT) {
                throw new BusinessException("该工单已被分配给其他维修工：" + ticket.getStaff().getNickname());
            }
            ticket.setStaff(staff);
            ticket.setAssignedAt(LocalDateTime.now());
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            appendStatusLog(ticket, oldStatus, TicketStatus.IN_PROGRESS, operator);
            return toDetailDto(ticket);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("工单已被其他用户修改，请刷新后重试");
        }
    }

    @Transactional
    public TicketDetailDto updateStatus(Long ticketId, TicketStatusUpdateRequest request) {
        try {
            // 使用悲观锁防止并发状态更新
            RepairTicket ticket = findTicketWithLock(ticketId);
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
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("工单已被其他用户修改，请刷新后重试");
        }
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
        try {
            // 使用悲观锁防止并发评价
            RepairTicket ticket = findTicketWithLock(ticketId);
            if (!Objects.equals(ticket.getStudent().getUserId(), request.studentId())) {
                throw new BusinessException("仅提交该报修单的学生可以评价");
            }
            if (ratingRepository.existsByTicket(ticket)) {
                throw new BusinessException("该报修单已评价");
            }

            // 允许在以下状态下评价：
            // - WAITING_FEEDBACK：正常待评价
            // - RESOLVED：已完成但还未进入待评价状态
            // - CLOSED：已关闭但尚未评价（例如管理员直接关闭）
            TicketStatus status = ticket.getStatus();
            if (status != TicketStatus.WAITING_FEEDBACK
                    && status != TicketStatus.RESOLVED
                    && status != TicketStatus.CLOSED) {
                throw new BusinessException("当前状态不可评价");
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
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("工单已被其他用户修改，请刷新后重试");
        }
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

    /**
     * 学生删除自己的报修单：物理删除（从数据库中彻底删除）
     * 仅允许删除待受理状态的工单
     */
    @Transactional
    public void deleteTicket(Long ticketId, String studentId) {
        try {
            User student = userService.loadActiveUser(studentId);
            // 使用悲观锁防止并发删除
            RepairTicket ticket = findTicketWithLock(ticketId);

            // 必须是本人提交的工单
            if (ticket.getStudent() == null ||
                    !Objects.equals(ticket.getStudent().getUserId(), student.getUserId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "只能删除自己提交的报修单");
            }

            // 仅允许删除待受理状态的工单
            if (ticket.getStatus() != TicketStatus.WAITING_ACCEPT) {
                throw new BusinessException("仅待受理状态的报修单可以删除");
            }

            // 先删除关联的图片
            List<TicketImage> images = ticket.getImages();
            if (images != null && !images.isEmpty()) {
                for (TicketImage image : images) {
                    // 删除文件系统中的图片文件
                    String imageUrl = image.getImageUrl();
                    if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                        try {
                            String filename = imageUrl.substring("/uploads/".length());
                            Path filePath = Paths.get(uploadPath, filename);
                            if (Files.exists(filePath)) {
                                Files.delete(filePath);
                                System.out.println("已删除图片文件: " + filePath);
                            }
                        } catch (IOException e) {
                            System.out.println("删除图片文件失败: " + e.getMessage());
                        }
                    }
                    // 删除数据库中的图片记录
                    imageRepository.delete(image);
                }
            }

            // 删除关联的状态日志
            List<TicketStatusLog> statusLogs = ticket.getStatusLogs();
            if (statusLogs != null && !statusLogs.isEmpty()) {
                statusLogRepository.deleteAll(statusLogs);
            }

            // 删除关联的评价（如果存在）
            Optional<Rating> ratingOpt = ratingRepository.findByTicket(ticket);
            if (ratingOpt.isPresent()) {
                ratingRepository.delete(ratingOpt.get());
            }

            // 最后删除工单本身（物理删除）
            ticketRepository.delete(ticket);
            ticketRepository.flush();

            System.out.println("已物理删除工单ID: " + ticketId);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("工单已被其他用户修改，请刷新后重试");
        }
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
        System.out.println("TicketService.listByStaff - 维修工ID: " + staffId);
        User staff = userService.loadActiveUser(staffId);
        System.out.println("找到用户: " + staff.getUserId() + ", 角色: " + staff.getRole());

        // 查询分配给该维修工的所有任务
        List<RepairTicket> tickets = ticketRepository.findByStaff(staff);
        System.out.println("数据库查询到的工单数量: " + tickets.size());

        if (tickets.isEmpty()) {
            System.out.println("警告：没有找到分配给维修工 " + staffId + " 的任务");
            // 检查数据库中是否有任何任务
            long totalTickets = ticketRepository.count();
            System.out.println("数据库中总任务数: " + totalTickets);
            // 检查有多少任务已经分配了维修工
            List<RepairTicket> allTickets = ticketRepository.findAll();
            long assignedCount = allTickets.stream()
                .filter(t -> t.getStaff() != null)
                .count();
            System.out.println("已分配的任务数: " + assignedCount);
            System.out.println("未分配的任务数: " + (totalTickets - assignedCount));
        } else {
            for (RepairTicket ticket : tickets) {
                System.out.println("工单ID: " + ticket.getTicketId() +
                                 ", 状态: " + ticket.getStatus() +
                                 ", 维修工ID: " + (ticket.getStaff() != null ? ticket.getStaff().getUserId() : "null"));
            }
        }

        return tickets.stream()
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
        return listAll(false);
    }

    @Transactional
    public List<TicketSummaryDto> listAll(boolean includeDeleted) {
        List<RepairTicket> tickets = ticketRepository.findAll();
        return tickets.stream()
            .filter(t -> includeDeleted || !Boolean.TRUE.equals(t.getDeleted()))
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
            .map(row -> {
                String id = (String) row[0];
                String name = (String) row[1];
                Number avgRatingNum = (Number) row[2];
                Number completedNum = (Number) row[3];
                int rating = avgRatingNum != null ? (int) Math.round(avgRatingNum.doubleValue()) : 0;
                int completed = completedNum != null ? completedNum.intValue() : 0;
                return new RepairmanRatingStatsDto(id, name, rating, completed);
            })
            // 限制返回前10条记录
            .limit(10)
            .collect(Collectors.toList());
    }

    // 新增方法：基于视图获取按类别的统计数据，供 DataAnalysis 使用
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryStatsFromView() {
        List<Object[]> results = ticketRepository.findCategoryStatsFromView();
        List<Map<String, Object>> stats = new ArrayList<>();

        for (Object[] row : results) {
            String categoryKey = (String) row[0];
            String categoryName = (String) row[1];
            // 原生 SQL 聚合函数在 MySQL 中通常返回 BigDecimal，这里统一用 Number 再转为整数，避免 ClassCastException
            Number totalTicketsNum = (Number) row[2];
            Number completedTicketsNum = (Number) row[3];
            Number ratedTicketsNum = (Number) row[4];
            Number avgRatingNum = (Number) row[5];

            Map<String, Object> item = new HashMap<>();
            // 前端 DataAnalysis 目前使用的字段
            item.put("category", categoryName);
            item.put("name", categoryName);
            item.put("type", categoryName);
            int total = totalTicketsNum != null ? totalTicketsNum.intValue() : 0;
            item.put("count", total);
            item.put("value", total);

            // 额外提供更详细的统计信息，便于后续扩展
            item.put("categoryKey", categoryKey);
            item.put("totalTickets", total);
            item.put("completedTickets", completedTicketsNum != null ? completedTicketsNum.intValue() : 0);
            item.put("ratedTickets", ratedTicketsNum != null ? ratedTicketsNum.intValue() : 0);
            item.put("avgRating", avgRatingNum != null ? avgRatingNum.doubleValue() : 0.0);

            stats.add(item);
        }

        return stats;
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

    // 新增方法：获取平均处理时间（从创建到完成）
    @Transactional(readOnly = true)
    public Map<String, Object> getAverageProcessingTime() {
        Double avgHours = ticketRepository.findAverageProcessingTimeHours();
        Long completedCount = ticketRepository.countCompletedTickets();

        Map<String, Object> result = new HashMap<>();
        result.put("completedTickets", completedCount);

        if (avgHours != null && avgHours > 0 && completedCount > 0) {
            // 将小时转换为更友好的格式
            double totalHours = avgHours;

            if (totalHours >= 24) {
                // 超过24小时，显示天数
                double days = totalHours / 24;
                result.put("avgHours", Math.round(totalHours));
                result.put("avgDays", Math.round(days * 10) / 10.0); // 保留一位小数
                result.put("displayText", String.format("约 %.1f 天", days));
            } else {
                // 少于24小时，显示小时
                result.put("avgHours", Math.round(totalHours));
                result.put("avgDays", 0);
                result.put("displayText", String.format("约 %.0f 小时", totalHours));
            }
        } else {
            result.put("avgHours", 0);
            result.put("avgDays", 0);
            result.put("displayText", "暂无数据");
        }

        return result;
    }

    private RepairTicket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "报修单不存在"));
    }

    /**
     * 使用悲观锁查找工单（用于关键操作，防止并发修改）
     */
    private RepairTicket findTicketWithLock(Long ticketId) {
        return ticketRepository.findByIdWithLock(ticketId)
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
                rating.getStudent() != null ? rating.getStudent().getNickname() : null,
                rating.getStaff() != null ? rating.getStaff().getUserId() : null,
                rating.getStaff() != null ? rating.getStaff().getNickname() : null,
                ticket.getTicketId(),
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
        // 查询该工单的评分（如果存在），用于给维修工统计平均分
        Integer ratingScore = ratingRepository.findByTicket(ticket)
            .map(Rating::getScore)
            .orElse(null);

        return new TicketSummaryDto(
            ticket.getTicketId(),
            ticket.getStatus(),
            ticket.getCategory() != null ? ticket.getCategory().getCategoryName() : null,
            ticket.getStudent() != null ? ticket.getStudent().getUserId() : null,
            ticket.getStaff() != null ? ticket.getStaff().getUserId() : null,
            ticket.getLocationText(),
            ticket.getDescription(),
            ticket.getPriority(),
            ticket.getCreatedAt(),
            ticket.getAssignedAt(),
            ticket.getEstimatedCompletionTime(),
            ratingScore,
            ticket.getDeleted(),
            ticket.getDeletedAt()
        );
    }
}
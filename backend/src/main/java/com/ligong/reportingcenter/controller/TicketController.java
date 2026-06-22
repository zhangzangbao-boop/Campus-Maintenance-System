package com.ligong.reportingcenter.controller;

import com.ligong.reportingcenter.domain.enums.TicketStatus;
import com.ligong.reportingcenter.dto.TicketDetailDto;
import com.ligong.reportingcenter.dto.TicketSummaryDto;
import com.ligong.reportingcenter.dto.request.TicketAssignRequest;
import com.ligong.reportingcenter.dto.request.TicketCreateRequest;
import com.ligong.reportingcenter.dto.request.TicketRatingRequest;
import com.ligong.reportingcenter.dto.request.TicketStatusUpdateRequest;
import com.ligong.reportingcenter.exception.BusinessException;
import com.ligong.reportingcenter.service.TicketService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    // 处理前端发送的 multipart/form-data 报修创建请求
    // 使用 @ModelAttribute 替代 @RequestPart，更宽松地接受 multipart 请求
    @PostMapping(
            value = "/repair-orders",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, "multipart/form-data"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createWithFiles(
            // 前端虽然会传 studentId，这里统一忽略，始终使用当前登录用户
            @RequestPart(value = "studentId", required = false) String ignoredStudentId,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "categoryId", required = false) String categoryIdStr,
            @RequestPart(value = "locationText", required = false) String locationText,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "priority", required = false) String priority,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        // 始终从 SecurityContext 获取当前登录用户，避免前端伪造 studentId
        String studentId;
        try {
            studentId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (studentId == null || studentId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }

        // 转换 categoryId
        Long categoryId;
        try {
            categoryId = Long.valueOf(categoryIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException("分类ID格式错误: " + categoryIdStr);
        }

        TicketCreateRequest request = new TicketCreateRequest(
            studentId,
            categoryId,
            locationText,
            description,
            priority,
            null
        );

        TicketDetailDto detail = ticketService.createTicket(request, images);

        // 为了兼容前端统一的 { code, data, message } 响应结构，这里做一层包装
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "报修创建成功");
        result.put("data", detail);
        return result;
    }

    @GetMapping("/repair-orders/my")
    public Map<String, Object> listMyTickets(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        // 从Security Context中获取当前用户ID
        String studentId;
        try {
            studentId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (studentId == null || studentId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }

        System.out.println("=== 查询学生报修单 ===");
        System.out.println("学生ID: " + studentId);
        System.out.println("状态筛选: " + status);

        // 先获取该学生的所有工单
        List<TicketSummaryDto> allTickets = ticketService.listByStudent(studentId);
        System.out.println("该学生的所有工单数量: " + allTickets.size());

        List<TicketSummaryDto> tickets;

        // 根据状态筛选
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            // 将前端状态值映射到后端状态值
            TicketStatus ticketStatus = mapStatusFromFrontend(status);
            System.out.println("映射后的状态: " + ticketStatus);

            // 在该学生的工单中筛选状态
            tickets = allTickets.stream()
                .filter(t -> t.status() == ticketStatus)
                .collect(java.util.stream.Collectors.toList());
            System.out.println("筛选后的工单数量: " + tickets.size());
        } else {
            // 不筛选状态，返回所有工单
            tickets = allTickets;
            System.out.println("返回所有工单，数量: " + tickets.size());
        }

        // 简单分页模拟
        int total = tickets.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TicketSummaryDto> pagedTickets = fromIndex < total ? tickets.subList(fromIndex, toIndex) : new java.util.ArrayList<>();

        System.out.println("分页后的工单数量: " + pagedTickets.size());
        System.out.println("===================");

        // 返回统一格式的响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        Map<String, Object> data = new HashMap<>();
        data.put("list", pagedTickets);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", size);
        result.put("data", data);
        return result;
    }

    @GetMapping("/repair-orders/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long id) {
        TicketDetailDto detail = ticketService.getTicketDetail(id);
        
        // 返回统一格式的响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", detail);
        return result;
    }

    @DeleteMapping("/repair-orders/{id}")
    public Map<String, Object> delete(@PathVariable("id") Long id) {
        // 从 SecurityContext 获取当前登录学生ID
        String studentId;
        try {
            studentId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (studentId == null || studentId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }

        ticketService.deleteTicket(id, studentId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除报修单成功");
        result.put("data", null);
        return result;
    }

    @PostMapping("/repair-orders/{id}/evaluate")
    public Map<String, Object> evaluate(@PathVariable("id") Long id,
                                        @Valid @RequestBody TicketRatingRequest request) {
        TicketDetailDto detail = ticketService.rateTicket(id, request);

        // 统一返回结构 { code, data, message }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "评价成功");
        result.put("data", detail);
        return result;
    }

    @PostMapping("/tasks/{id}/assign")
    public TicketDetailDto assign(@PathVariable("id") Long id,
                                  @Valid @RequestBody TicketAssignRequest request) {
        return ticketService.assignTicket(id, request);
    }

    @PutMapping("/tasks/{id}/status")
    public Map<String, Object> changeTaskStatus(@PathVariable("id") Long id,
                                          @RequestBody Map<String, Object> requestBody) {
        // 从 SecurityContext 获取当前操作员ID
        String operatorId;
        try {
            operatorId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (operatorId == null || operatorId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }
        
        // 从请求体中获取状态
        String newStatusStr = (String) requestBody.get("newStatus");
        if (newStatusStr == null || newStatusStr.isBlank()) {
            throw new BusinessException("新状态不能为空");
        }
        
        // 将字符串状态转换为枚举
        TicketStatus newStatus;
        try {
            newStatus = TicketStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            // 尝试从前端状态值映射
            newStatus = mapStatusFromFrontend(newStatusStr);
        }
        
        String rejectionReason = (String) requestBody.get("rejectionReason");
        
        TicketStatusUpdateRequest request = new TicketStatusUpdateRequest(
            operatorId,
            newStatus,
            rejectionReason
        );
        
        TicketDetailDto detail = ticketService.updateStatus(id, request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "任务状态更新成功");
        result.put("data", detail);
        return result;
    }

    @PutMapping("/tasks/{id}/complete")
    public Map<String, Object> completeTask(@PathVariable("id") Long id,
                                      @RequestBody Map<String, Object> requestBody) {
        // 从 SecurityContext 获取当前操作员ID
        String operatorId;
        try {
            operatorId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (operatorId == null || operatorId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }
        
        // 完成任务时，状态应该是 RESOLVED
        String rejectionReason = (String) requestBody.get("rejectionReason");
        
        TicketStatusUpdateRequest request = new TicketStatusUpdateRequest(
            operatorId,
            TicketStatus.RESOLVED,
            rejectionReason
        );
        
        TicketDetailDto detail = ticketService.updateStatus(id, request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "任务完成成功");
        result.put("data", detail);
        return result;
    }

    @GetMapping("/tasks/my")
    public Map<String, Object> listMyTasks(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        // 从Security Context中获取当前维修工ID
        String staffId;
        try {
            staffId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (staffId == null || staffId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }
        
        System.out.println("=== 查询维修工任务 ===");
        System.out.println("维修工ID: " + staffId);
        System.out.println("状态筛选: " + status);
        
        List<TicketSummaryDto> tasks;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try {
                // 将前端状态值映射到后端枚举值
                TicketStatus ticketStatus = mapStatusFromFrontend(status);
                System.out.println("映射后的状态: " + ticketStatus);
                // 先获取该状态的所有任务，然后过滤出分配给当前维修工的
                List<TicketSummaryDto> allStatusTasks = ticketService.listByStatus(ticketStatus);
                System.out.println("该状态的所有任务数量: " + allStatusTasks.size());
                // 过滤出分配给当前维修工的任务
                tasks = allStatusTasks.stream()
                    .filter(task -> {
                        boolean matches = staffId.equals(task.staffId());
                        if (!matches) {
                            System.out.println("任务 " + task.ticketId() + " 的维修工ID: " + task.staffId() + " (不匹配)");
                        }
                        return matches;
                    })
                    .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("无效的任务状态: " + status);
            }
        } else {
            // 获取分配给当前维修工的所有任务
            System.out.println("查询分配给维修工的所有任务（status为all或null）...");
            tasks = ticketService.listByStaff(staffId);
            System.out.println("查询结果数量: " + tasks.size());
            if (tasks.isEmpty()) {
                System.out.println("警告：没有找到分配给维修工 " + staffId + " 的任务");
                System.out.println("可能原因：1. 数据库中确实没有分配给该维修工的任务");
                System.out.println("         2. 需要管理员先分配任务给该维修工");
            }
        }
        
        System.out.println("最终返回的任务数量: " + tasks.size());
        System.out.println("===================");
        
        // 简单分页模拟
        int total = tasks.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TicketSummaryDto> pagedTasks = fromIndex < total ? tasks.subList(fromIndex, toIndex) : new java.util.ArrayList<>();
        
        // 返回统一格式的响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        Map<String, Object> data = new HashMap<>();
        data.put("list", pagedTasks);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", size);
        result.put("data", data);
        return result;
    }

    @GetMapping("/tasks/{id}")
    public Map<String, Object> taskDetail(@PathVariable("id") Long id) {
        TicketDetailDto detail = ticketService.getTicketDetail(id);
        
        // 返回统一格式的响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", detail);
        return result;
    }

    @GetMapping("/admin/repair-orders")
    public Map<String, Object> listAllTickets(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {

        // 先取出所有工单摘要（根据 includeDeleted 参数决定是否包含已删除的）
        List<TicketSummaryDto> allTickets = ticketService.listAll(includeDeleted);

        // 按照前端传入的条件在内存中过滤（数据量不大时足够）
        java.util.stream.Stream<TicketSummaryDto> stream = allTickets.stream();

        // 1. 状态筛选（前端传的是 pending/processing/...）
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            com.ligong.reportingcenter.domain.enums.TicketStatus targetStatus = mapStatusFromFrontend(status);
            stream = stream.filter(t -> t.status() == targetStatus);
        }

        // 2. 分类筛选（前端传的是 categoryName / categoryKey，当前我们用 categoryName 直接对比）
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            stream = stream.filter(t -> category.equalsIgnoreCase(t.categoryName()));
        }

        // 3. 关键词筛选（在描述、位置、工单ID、学生ID、维修工ID、分类名称中模糊匹配）
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase().trim();
            final String searchKeyword = lower; // 用于lambda表达式
            stream = stream.filter(t -> {
                String desc = t.description() != null ? t.description().toLowerCase() : "";
                String loc = t.locationText() != null ? t.locationText().toLowerCase() : "";
                String ticketIdStr = t.ticketId() != null ? String.valueOf(t.ticketId()) : "";
                String studentIdStr = t.studentId() != null ? t.studentId().toLowerCase() : "";
                String staffIdStr = t.staffId() != null ? t.staffId().toLowerCase() : "";
                String categoryNameStr = t.categoryName() != null ? t.categoryName().toLowerCase() : "";
                boolean matches = desc.contains(searchKeyword) 
                    || loc.contains(searchKeyword)
                    || ticketIdStr.contains(searchKeyword)
                    || studentIdStr.contains(searchKeyword)
                    || staffIdStr.contains(searchKeyword)
                    || categoryNameStr.contains(searchKeyword);
                return matches;
            });
        }

        List<TicketSummaryDto> filtered = stream.toList();

        // 简单分页模拟
        int total = filtered.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TicketSummaryDto> pagedTickets =
                fromIndex < total ? filtered.subList(fromIndex, toIndex) : new java.util.ArrayList<>();

        // 返回统一格式，兼容前端对 { code, data: { list, total, page, pageSize } } 的处理
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        Map<String, Object> data = new HashMap<>();
        data.put("list", pagedTickets);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", size);
        result.put("data", data);
        return result;
    }

    @PutMapping("/admin/repair-orders/{id}/assign")
    public Map<String, Object> adminAssign(@PathVariable("id") Long id,
                                     @Valid @RequestBody Map<String, String> requestBody) {
        // 从 SecurityContext 获取当前操作员ID
        String operatorId;
        try {
            operatorId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (operatorId == null || operatorId.isBlank()) {
                throw new BusinessException("无法获取当前用户信息，请先登录");
            }
        } catch (Exception e) {
            throw new BusinessException("无法获取当前用户信息，请先登录");
        }
        
        // 从请求体中获取维修工ID
        String staffId = requestBody.get("repairmanId") != null ? requestBody.get("repairmanId") : requestBody.get("staffId");
        if (staffId == null || staffId.isBlank()) {
            throw new BusinessException("维修工ID不能为空");
        }
        
        // 构造分配请求
        TicketAssignRequest request = new TicketAssignRequest(operatorId, staffId);
        TicketDetailDto detail = ticketService.assignTicket(id, request);
        
        // 返回统一格式
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "工单分配成功");
        result.put("data", detail);
        return result;
    }

    @PutMapping("/admin/repair-orders/{id}/status")
    public TicketDetailDto adminChangeStatus(@PathVariable("id") Long id,
                                           @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ticketService.updateStatus(id, request);
    }
    
    // 添加新接口：更新维修备注
    @PutMapping("/admin/repair-orders/{id}/repair-notes")
    public TicketDetailDto updateRepairNotes(@PathVariable("id") Long id,
                                           @RequestBody UpdateNotesRequest request) {
        // TODO: 从Security Context中获取当前操作员ID
        String operatorId = "admin";
        return ticketService.updateRepairNotes(id, request.notes(), operatorId);
    }
    
    // 添加新接口：更新处理备注
    @PutMapping("/admin/repair-orders/{id}/process-notes")
    public TicketDetailDto updateProcessNotes(@PathVariable("id") Long id,
                                            @RequestBody UpdateNotesRequest request) {
        // TODO: 从Security Context中获取当前操作员ID
        String operatorId = "admin";
        return ticketService.updateProcessNotes(id, request.notes(), operatorId);
    }
    
    // 添加新接口：设置预计完成时间
    @PutMapping("/admin/repair-orders/{id}/estimated-completion-time")
    public TicketDetailDto setEstimatedCompletionTime(@PathVariable("id") Long id,
                                                   @RequestBody SetEstimatedTimeRequest request) {
        // TODO: 从Security Context中获取当前操作员ID
        String operatorId = "admin";
        return ticketService.setEstimatedCompletionTime(id, request.estimatedTime(), operatorId);
    }
    
    // 将前端状态值映射到后端枚举值
    private TicketStatus mapStatusFromFrontend(String frontendStatus) {
        String status = frontendStatus.toLowerCase();
        return switch (status) {
            case "pending" -> TicketStatus.WAITING_ACCEPT;
            case "processing" -> TicketStatus.IN_PROGRESS;
            case "completed", "resolved" -> TicketStatus.RESOLVED;
            case "to_be_evaluated", "waiting_feedback" -> TicketStatus.WAITING_FEEDBACK;
            case "closed" -> TicketStatus.CLOSED;
            case "rejected" -> TicketStatus.REJECTED;
            default -> {
                // 尝试直接转换为枚举值
                try {
                    yield TicketStatus.valueOf(frontendStatus.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("无效的任务状态: " + frontendStatus);
                }
            }
        };
    }
    
    // 内部请求类
    public record UpdateNotesRequest(String notes) {}
    
    public record SetEstimatedTimeRequest(java.time.LocalDateTime estimatedTime) {}
}
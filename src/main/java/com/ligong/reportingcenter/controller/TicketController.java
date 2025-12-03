package com.ligong.reportingcenter.controller;

import com.ligong.reportingcenter.domain.enums.TicketStatus;
import com.ligong.reportingcenter.dto.PagedResult;
import com.ligong.reportingcenter.dto.TicketDetailDto;
import com.ligong.reportingcenter.dto.TicketSummaryDto;
import com.ligong.reportingcenter.dto.request.TicketAssignRequest;
import com.ligong.reportingcenter.dto.request.TicketCreateRequest;
import com.ligong.reportingcenter.dto.request.TicketImageRequest;
import com.ligong.reportingcenter.dto.request.TicketRatingRequest;
import com.ligong.reportingcenter.dto.request.TicketStatusUpdateRequest;
import com.ligong.reportingcenter.exception.BusinessException;
import com.ligong.reportingcenter.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/repair-orders")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetailDto create(@Valid @RequestBody TicketCreateRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping("/repair-orders/my")
    public PagedResult<TicketSummaryDto> listMyTickets(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        // TODO: 从Security Context中获取当前用户ID
        String studentId = "test_student"; // 示例值
        
        List<TicketSummaryDto> tickets;
        if (status != null && !status.isBlank()) {
            try {
                TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
                tickets = ticketService.listByStatus(ticketStatus);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("无效的工单状态");
            }
        } else {
            tickets = ticketService.listByStudent(studentId);
        }
        
        // 简单分页模拟
        int total = tickets.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TicketSummaryDto> pagedTickets = tickets.subList(fromIndex, toIndex);
        
        return new PagedResult<>(pagedTickets, total);
    }

    @GetMapping("/repair-orders/{id}")
    public TicketDetailDto detail(@PathVariable("id") Long id) {
        return ticketService.getTicketDetail(id);
    }

    @DeleteMapping("/repair-orders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        // TODO: 实现删除工单逻辑（仅允许删除pending状态的工单）
    }

    @PostMapping("/repair-orders/{id}/evaluate")
    public TicketDetailDto evaluate(@PathVariable("id") Long id,
                                  @Valid @RequestBody TicketRatingRequest request) {
        return ticketService.rateTicket(id, request);
    }

    @PostMapping("/tasks/{id}/assign")
    public TicketDetailDto assign(@PathVariable("id") Long id,
                                  @Valid @RequestBody TicketAssignRequest request) {
        return ticketService.assignTicket(id, request);
    }

    @PutMapping("/tasks/{id}/status")
    public TicketDetailDto changeTaskStatus(@PathVariable("id") Long id,
                                          @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ticketService.updateStatus(id, request);
    }

    @PutMapping("/tasks/{id}/complete")
    public TicketDetailDto completeTask(@PathVariable("id") Long id,
                                      @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ticketService.updateStatus(id, request);
    }

    @GetMapping("/tasks/my")
    public PagedResult<TicketSummaryDto> listMyTasks(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        // TODO: 从Security Context中获取当前维修工ID
        String staffId = "test_staff"; // 示例值
        
        List<TicketSummaryDto> tasks;
        if (status != null && !status.isBlank()) {
            try {
                TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
                tasks = ticketService.listByStatus(ticketStatus);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("无效的任务状态");
            }
        } else {
            tasks = ticketService.listByStaff(staffId);
        }
        
        // 简单分页模拟
        int total = tasks.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TicketSummaryDto> pagedTasks = tasks.subList(fromIndex, toIndex);
        
        return new PagedResult<>(pagedTasks, total);
    }

    @GetMapping("/tasks/{id}")
    public TicketDetailDto taskDetail(@PathVariable("id") Long id) {
        return ticketService.getTicketDetail(id);
    }

    @GetMapping("/admin/repair-orders")
    public PagedResult<TicketSummaryDto> listAllTickets(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<TicketSummaryDto> allTickets = ticketService.listAll();
        
        // 简单分页模拟
        int total = allTickets.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TicketSummaryDto> pagedTickets = allTickets.subList(fromIndex, toIndex);
        
        return new PagedResult<>(pagedTickets, total);
    }

    @PutMapping("/admin/repair-orders/{id}/assign")
    public TicketDetailDto adminAssign(@PathVariable("id") Long id,
                                     @Valid @RequestBody TicketAssignRequest request) {
        return ticketService.assignTicket(id, request);
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
    
    // 内部请求类
    public record UpdateNotesRequest(String notes) {}
    
    public record SetEstimatedTimeRequest(java.time.LocalDateTime estimatedTime) {}
}
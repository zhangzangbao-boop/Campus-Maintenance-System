package com.ligong.reportingcenter.controller;

import com.ligong.reportingcenter.dto.PagedResult;
import com.ligong.reportingcenter.dto.RatingDto;
import com.ligong.reportingcenter.dto.ResetPasswordResult;
import com.ligong.reportingcenter.dto.UserDto;
import com.ligong.reportingcenter.dto.request.UserRegisterRequest;
import com.ligong.reportingcenter.service.UserService;
import com.ligong.reportingcenter.service.TicketService;
import com.ligong.reportingcenter.service.RatingService;
import com.ligong.reportingcenter.service.CategoryService;
import com.ligong.reportingcenter.domain.enums.TicketStatus;
import com.ligong.reportingcenter.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TicketService ticketService;
    private final RatingService ratingService;
    private final CategoryService categoryService;

    // 用户管理
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResult<UserDto> listUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<UserDto> allUsers = userService.listAll();
        
        // 简单分页模拟
        int total = allUsers.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<UserDto> pagedUsers = allUsers.subList(fromIndex, toIndex);
        
        return new PagedResult<>(pagedUsers, total);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@RequestBody UserRegisterRequest request) {
        return userService.register(request);
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUser(@PathVariable("userId") String userId, @RequestBody UserRegisterRequest request) {
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("userId") String userId) {
        userService.deactivate(userId);
    }

    // 评价管理
    @GetMapping("/feedbacks")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResult<RatingDto> listFeedbacks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<RatingDto> allRatings = ratingService.listAll();
        
        // 简单分页模拟
        int total = allRatings.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<RatingDto> pagedRatings = allRatings.subList(fromIndex, toIndex);
        
        return new PagedResult<>(pagedRatings, total);
    }

    @DeleteMapping("/feedbacks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFeedback(@PathVariable Long id) {
        ratingService.delete(id);
    }

    // 数据统计
    @GetMapping("/stats/category")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getCategoryStats() {
        // 基于数据库视图 vw_category_stats 的统计结果
        List<Map<String, Object>> categoryStats = ticketService.getCategoryStatsFromView();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", categoryStats);
        return result;
    }

    @GetMapping("/stats/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getStatusStats() {
        List<Map<String, Object>> statusStats = new java.util.ArrayList<>();
        for (TicketStatus status : TicketStatus.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", status.name());
            item.put("count", ticketService.countByStatus(status));
            item.put("value", ticketService.countByStatus(status));
            statusStats.add(item);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", statusStats);
        return result;
    }

    @GetMapping("/stats/location")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getLocationStats() {
        List<com.ligong.reportingcenter.dto.LocationStatsDto> locationStats = ticketService.getLocationStats();
        List<Map<String, Object>> locationData = locationStats.stream()
            .map(dto -> {
                Map<String, Object> item = new HashMap<>();
                item.put("location", dto.location());
                item.put("name", dto.location());
                item.put("count", dto.count());
                item.put("value", dto.count());
                return item;
            })
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", locationData);
        return result;
    }

    @GetMapping("/stats/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getMonthlyStats() {
        Map<String, Object> monthlyStats = ticketService.getMonthlyStats();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", monthlyStats);
        return result;
    }

    @GetMapping("/stats/repairman-rating")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getRepairmanRatingStats() {
        List<com.ligong.reportingcenter.dto.RepairmanRatingStatsDto> ratingStats = ticketService.getRepairmanRatingStats();
        List<Map<String, Object>> ratingData = ratingStats.stream()
            .map(dto -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", dto.id());
                item.put("name", dto.name());
                item.put("rating", dto.rating());
                item.put("completedOrders", dto.completedOrders());
                item.put("count", dto.completedOrders());
                return item;
            })
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", ratingData);
        return result;
    }

    // 新增方法：重置用户密码
    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable("userId") String userId) {
        try {
            ResetPasswordResult result = userService.resetPassword(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "密码重置成功");
            response.put("userId", result.getUserId());
            response.put("newPassword", result.getNewPassword());
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

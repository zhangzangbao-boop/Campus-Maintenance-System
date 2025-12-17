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
import com.ligong.reportingcenter.domain.entity.Category;
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
    public UserDto updateUser(@PathVariable String userId, @RequestBody UserRegisterRequest request) {
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String userId) {
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
    public Object getCategoryStats() {
        List<Category> categories = categoryService.listAllCategories();
        Map<String, Long> categoryStats = new HashMap<>();
        for (Category category : categories) {
            categoryStats.put(category.getCategoryName(), 
                ticketService.countByCategory(category.getCategoryId()));
        }
        return categoryStats;
    }

    @GetMapping("/stats/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Object getStatusStats() {
        Map<String, Long> statusStats = new HashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            statusStats.put(status.name(), ticketService.countByStatus(status));
        }
        return statusStats;
    }

    @GetMapping("/stats/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public Object getMonthlyStats() {
        return ticketService.getMonthlyStats();
    }

    // 新增方法：重置用户密码
    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable String userId) {
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

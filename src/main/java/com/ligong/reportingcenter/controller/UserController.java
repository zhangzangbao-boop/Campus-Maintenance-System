package com.ligong.reportingcenter.controller;

import com.ligong.reportingcenter.domain.enums.UserRole;
import com.ligong.reportingcenter.dto.UserDto;
import com.ligong.reportingcenter.dto.request.UserUpdateRequest;
import com.ligong.reportingcenter.exception.BusinessException;
import com.ligong.reportingcenter.service.UserService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserDto getCurrentUser() {
        // 从Security Context中获取当前用户ID
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getCurrentUser(currentUserId);
    }

    @PutMapping("/me")
    public UserDto updateUserInfo(@RequestBody UserUpdateRequest request) {
        // 从Security Context中获取当前用户ID
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateUserInfo(currentUserId, request);
    }

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable("userId") String userId) {
        return userService.findById(userId);
    }

    @GetMapping
    public List<UserDto> listUsers(@RequestParam(value = "role", required = false) String role) {
        if (role == null || role.isBlank()) {
            return userService.listAll();
        }
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return userService.listByRole(userRole);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("无效的角色类型");
        }
    }

    @PatchMapping("/{userId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("userId") @NotBlank String userId) {
        userService.deactivate(userId);
    }
    
    @PatchMapping("/{userId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable("userId") @NotBlank String userId) {
        userService.activate(userId);
    }
}
package com.ligong.reportingcenter.service;

import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.enums.UserRole;
import com.ligong.reportingcenter.dto.ResetPasswordResult;
import com.ligong.reportingcenter.dto.UserDto;
import com.ligong.reportingcenter.dto.request.LoginRequest;
import com.ligong.reportingcenter.dto.request.UserRegisterRequest;
import com.ligong.reportingcenter.dto.request.UserUpdateRequest;
import com.ligong.reportingcenter.dto.response.AuthResponse;
import com.ligong.reportingcenter.exception.BusinessException;
import com.ligong.reportingcenter.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto register(UserRegisterRequest request) {
        if (userRepository.existsByUserId(request.userId())) {
            throw new BusinessException("用户ID已存在");
        }
        User user = new User();
        user.setUserId(request.userId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setContactPhone(request.contactPhone());
        user.setRole(request.role());
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUserIdAndIsActiveTrue(request.userId())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "账号不存在或已禁用"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "密码错误");
        }
        return new AuthResponse(toDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto findById(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> listByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrue(role)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> listAll() {
        return userRepository.findAll()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deactivate(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setIsActive(false);
    }

    @Transactional
    public UserDto updateUser(String userId, UserRegisterRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        
        // 更新用户信息
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname());
        }
        
        if (request.contactPhone() != null && !request.contactPhone().isBlank()) {
            user.setContactPhone(request.contactPhone());
        }
        
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public void activate(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setIsActive(true);
    }

    private UserDto toDto(User user) {
        return new UserDto(
            user.getUserId(),
            user.getNickname(),
            user.getContactPhone(),
            user.getRole(),
            Boolean.TRUE.equals(user.getIsActive())
        );
    }

    public User loadActiveUser(String userId) {
        return userRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在或已禁用"));
    }

    @Transactional
    public UserDto updateUserInfo(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        
        // 验证用户角色权限
        if (user.getRole() != UserRole.STUDENT && user.getRole() != UserRole.STAFF) {
            throw new BusinessException("仅学生和维修工可以更新个人信息");
        }
        
        // 更新用户信息
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname());
        }
        
        if (request.getContactPhone() != null && !request.getContactPhone().isBlank()) {
            user.setContactPhone(request.getContactPhone());
        }
        
        // 头像上传处理（如果需要）
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        userRepository.save(user);
        return toDto(user);
    }

    // 新增方法：获取当前用户信息
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        return toDto(user);
    }

    @Transactional
    public ResetPasswordResult resetPassword(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        
        // 生成随机密码
        String newPassword = generateRandomPassword();
        
        // 验证密码复杂度
        validatePasswordComplexity(newPassword);
        
        // 加密并保存新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // 发送密码重置通知（可选）
        sendPasswordResetNotification(user.getUserId(), newPassword);
        
        return new ResetPasswordResult(user.getUserId(), newPassword);
    }

    private void validatePasswordComplexity(String password) {
        if (password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位");
        }
        
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("密码必须包含字母和数字");
        }
    }

    private String generateRandomPassword() {
        // 生成8位随机密码，确保包含字母和数字
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        
        // 确保至少有一个字母和一个数字
        password.append(RandomStringUtils.random(1, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"));
        password.append(RandomStringUtils.random(1, "0123456789"));
        
        // 剩余6位随机生成
        password.append(RandomStringUtils.random(6, chars));
        
        return password.toString();
    }

    private void sendPasswordResetNotification(String userId, String newPassword) {
        // 实现邮件或短信通知逻辑
        // 这里只是示例
        System.out.println("发送密码重置通知给用户: " + userId + ", 新密码: " + newPassword);
    }
}


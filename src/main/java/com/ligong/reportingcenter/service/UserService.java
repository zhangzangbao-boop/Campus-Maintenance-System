package com.ligong.reportingcenter.service;

import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.enums.UserRole;
import com.ligong.reportingcenter.dto.UserDto;
import com.ligong.reportingcenter.dto.request.LoginRequest;
import com.ligong.reportingcenter.dto.request.UserRegisterRequest;
import com.ligong.reportingcenter.dto.response.AuthResponse;
import com.ligong.reportingcenter.exception.BusinessException;
import com.ligong.reportingcenter.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
}


package com.ligong.reportingcenter.domain.entity;

import com.ligong.reportingcenter.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user") // 修改为与数据库一致
public class User {

    @Id
    @Column(name = "user_number") // 修改为主键列名与数据库一致
    private String userId;

    @Column(name = "name", nullable = false, length = 100) // 修改列名与长度
    private String nickname;

    @Column(name = "password", nullable = false, length = 255) // 修改列名与长度
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20) // 修改列名与长度
    private UserRole role;

    @Column(name = "enabled") // 修改列名
    private Boolean isActive = true;

    @Column(name = "phone", length = 20) // 修改列名与长度
    private String contactPhone;

    @Column(name = "created_at", nullable = false) // 修改列名
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}

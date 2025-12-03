package com.ligong.reportingcenter.repository;

import com.ligong.reportingcenter.domain.entity.User;
import com.ligong.reportingcenter.domain.enums.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserIdAndIsActiveTrue(String userId);
    boolean existsByUserId(String userId);
    List<User> findByRoleAndIsActiveTrue(UserRole role);
}


package com.dormhub.repository;

import com.dormhub.entity.User;
import com.dormhub.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /** Danh sách user theo vai trò (dùng để thông báo cho toàn bộ Manager). */
    List<User> findAllByRole(Role role);
}
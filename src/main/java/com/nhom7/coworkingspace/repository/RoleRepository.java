package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository dùng để thao tác với bảng role.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tìm role theo tên.
     *
     * Ví dụ:
     * findByName("MODERATOR")
     */
    Optional<Role> findByName(String name);
}
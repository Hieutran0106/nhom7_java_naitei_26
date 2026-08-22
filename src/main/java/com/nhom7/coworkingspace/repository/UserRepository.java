package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}

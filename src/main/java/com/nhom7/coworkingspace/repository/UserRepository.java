package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository cho entity User.
 *
 * <p>Spring Data JPA tự sinh implementation lúc runtime — không cần viết SQL thủ công.
 * Tên method tuân theo convention "findBy + FieldName" → Spring tự tạo query đúng (S017 ✅).</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email (dùng khi login / load UserDetails).
     *
     * @param email email cần tra cứu
     * @return Optional chứa User nếu tìm thấy, empty nếu không
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra email đã tồn tại trong DB chưa (dùng khi đăng ký).
     *
     * @param email email cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsByEmail(String email);
}

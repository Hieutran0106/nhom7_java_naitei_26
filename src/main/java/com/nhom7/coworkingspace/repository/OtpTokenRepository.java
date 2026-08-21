package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.enums.OtpPurpose;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findByUserAndPurpose(User user, OtpPurpose purpose);

    void deleteByUserAndPurpose(User user, OtpPurpose purpose);
}

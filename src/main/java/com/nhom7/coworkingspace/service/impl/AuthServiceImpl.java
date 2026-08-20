package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.request.SignupRequest;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.FileStorageService;
import com.nhom7.coworkingspace.service.OtpService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException("user.email.exists", HttpStatus.CONFLICT);
        }

        String cccdPath = fileStorageService.storeFile(request.getCccdImage(), "cccd");

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        String normalizedName = normalizeName(request.getName());

        User user = User.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone().trim())
                .status(UserStatus.INACTIVE)
                .isIdentityVerified(false)
                .isBusinessVerified(false)
                .language("vi")
                .cccdUrl(cccdPath)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        User savedUser = userRepository.save(user);
        otpService.sendConfirmationOtp(normalizedEmail);
        log.info("User registered successfully with id: {}", savedUser.getId());

        Set<String> roleNames = savedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return SignupResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .status(savedUser.getStatus())
                .isIdentityVerified(savedUser.getIsIdentityVerified())
                .language(savedUser.getLanguage())
                .cccdUrl(savedUser.getCccdUrl())
                .roles(roleNames)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException("auth.invalid.credentials", HttpStatus.UNAUTHORIZED));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException("auth.account.blocked", HttpStatus.FORBIDDEN);
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .isIdentityVerified(user.getIsIdentityVerified())
                .language(user.getLanguage())
                .roles(roleNames)
                .build();
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                Date expiry = jwtTokenProvider.extractExpiration(token);
                tokenBlacklistService.blacklistToken(token, expiry);
                log.info("[Auth] Token successfully added to blacklist upon logout");
            }
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String[] words = name.trim().split("\\s+");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                normalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
                if (i < words.length - 1) {
                    normalized.append(" ");
                }
            }
        }
        return normalized.toString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

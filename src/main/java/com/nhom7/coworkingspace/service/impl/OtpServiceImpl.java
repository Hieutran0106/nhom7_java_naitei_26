package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.OtpPurpose;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.OtpTokenRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.EmailService;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import com.nhom7.coworkingspace.service.OtpService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.util.OtpCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final OtpCodeGenerator otpCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AppOtpProperties otpProperties;
    private final Clock clock;
    private final MessageSource messageSource;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public void sendConfirmationOtp(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null || user.getStatus() != UserStatus.INACTIVE) {
            return;
        }

        createAndSendOtp(user, OtpPurpose.ACCOUNT_CONFIRMATION);
    }

    @Override
    @Transactional
    public void sendPasswordResetOtp(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        createAndSendOtp(user, OtpPurpose.PASSWORD_RESET);
    }

    private void createAndSendOtp(User user, OtpPurpose purpose) {
        Instant now = clock.instant();

        Optional<OtpToken> existingOpt = otpTokenRepository.findByUserAndPurpose(user, purpose);
        if (existingOpt.isPresent()) {
            OtpToken existingToken = existingOpt.get();
            long elapsedSeconds = Duration.between(existingToken.getCreatedAt(), now).getSeconds();
            if (elapsedSeconds < otpProperties.getResendCooldownSeconds()) {
                throw new AppException("auth.otp.cooldown", HttpStatus.TOO_MANY_REQUESTS);
            }
            otpTokenRepository.delete(existingToken);
        }

        String code = otpCodeGenerator.generateCode();
        OtpToken token = OtpToken.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .purpose(purpose)
                .createdAt(now)
                .expiresAt(now.plusSeconds(otpProperties.getExpirationMinutes() * 60))
                .failedAttempts(0)
                .build();
        otpTokenRepository.saveAndFlush(token);

        Locale locale = toLocale(user.getLanguage());
        String subjectKey = purpose == OtpPurpose.ACCOUNT_CONFIRMATION
                ? "email.confirmation.subject"
                : "email.password.reset.subject";
        String subject = messageSource.getMessage(subjectKey, null, locale);
        String html = purpose == OtpPurpose.ACCOUNT_CONFIRMATION
                ? emailTemplateService.renderAccountConfirmation(code, locale)
                : emailTemplateService.renderPasswordReset(code, locale);
        emailService.sendHtmlEmail(user.getEmail(), subject, html);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Locale toLocale(String language) {
        if (!StringUtils.hasText(language)) {
            return Locale.ENGLISH;
        }
        return Locale.forLanguageTag(language.replace('_', '-'));
    }

    @Override
    @Transactional
    public void confirmAccount(String email, String otp) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otp)) {
            throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
        }

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST));

        if (user.getStatus() != UserStatus.INACTIVE) {
            throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
        }

        OtpToken token = otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION)
                .orElseThrow(() -> new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST));

        Instant now = clock.instant();
        if (token.getExpiresAt().isBefore(now)) {
            otpTokenRepository.delete(token);
            throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(otp.trim(), token.getCodeHash())) {
            handleFailedAttempt(token);
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Delete used token to prevent replay attacks
        otpTokenRepository.delete(token);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otp) || !StringUtils.hasText(newPassword)) {
            throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
        }

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
        }

        OtpToken token = otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST));

        Instant now = clock.instant();
        if (token.getExpiresAt().isBefore(now)) {
            otpTokenRepository.delete(token);
            throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(otp.trim(), token.getCodeHash())) {
            handleFailedAttempt(token);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        // Invalidate all tokens previously issued for this user
        tokenBlacklistService.blacklistUserTokens(user.getEmail(), Date.from(now));

        // Delete used token to prevent replay attacks
        otpTokenRepository.delete(token);
    }

    private void handleFailedAttempt(OtpToken token) {
        int attempts = token.getFailedAttempts() + 1;
        token.setFailedAttempts(attempts);

        if (attempts >= otpProperties.getMaxFailedAttempts()) {
            otpTokenRepository.delete(token);
            throw new AppException("auth.otp.max_attempts_exceeded", HttpStatus.BAD_REQUEST);
        }

        otpTokenRepository.save(token);
        throw new AppException("auth.otp.invalid", HttpStatus.BAD_REQUEST);
    }
}

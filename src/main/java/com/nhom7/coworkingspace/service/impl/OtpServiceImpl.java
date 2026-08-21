package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.OtpPurpose;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.repository.OtpTokenRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.EmailService;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import com.nhom7.coworkingspace.service.OtpService;
import com.nhom7.coworkingspace.util.OtpCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

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
        otpTokenRepository.deleteByUserAndPurpose(user, purpose);

        String code = otpCodeGenerator.generateCode();
        Instant now = clock.instant();
        OtpToken token = OtpToken.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .purpose(purpose)
                .createdAt(now)
                .expiresAt(now.plusSeconds(otpProperties.getExpirationMinutes() * 60))
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
}

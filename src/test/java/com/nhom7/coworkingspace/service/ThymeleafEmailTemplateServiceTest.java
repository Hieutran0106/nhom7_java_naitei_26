package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.service.impl.ThymeleafEmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafEmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        templateEngine.setTemplateEngineMessageSource(messageSource);
        AppOtpProperties otpProperties = new AppOtpProperties();
        otpProperties.setExpirationMinutes(7);
        emailTemplateService = new ThymeleafEmailTemplateService(templateEngine, otpProperties);
    }

    @Test
    void renderAccountConfirmationShouldIncludeOtpCode() {
        String html = emailTemplateService.renderAccountConfirmation("123456", Locale.ENGLISH);

        assertThat(html)
                .contains("Confirm your account")
                .contains("123456")
                .contains("This code expires in 7 minutes.");
    }

    @Test
    void renderPasswordResetShouldIncludeOtpCode() {
        String html = emailTemplateService.renderPasswordReset(
                "654321", Locale.forLanguageTag("vi"));

        assertThat(html)
                .contains("Đặt lại mật khẩu của bạn")
                .contains("654321")
                .contains("Mã này sẽ hết hạn sau 7 phút.");
    }

    @Test
    void renderBookingStatusChangedShouldIncludeBookingDetails() {
        User user = User.builder().name("Nguyen Van A").build();
        Space space = Space.builder().name("Meeting Room A").build();
        Booking booking = Booking.builder()
                .id(42L)
                .user(user)
                .space(space)
                .startTime(LocalDateTime.of(2026, 8, 22, 9, 0))
                .endTime(LocalDateTime.of(2026, 8, 22, 11, 0))
                .totalPrice(new BigDecimal("250000.00"))
                .status("APPROVED")
                .build();

        String html = emailTemplateService.renderBookingStatusChanged(
                booking, "PENDING", Locale.ENGLISH);

        assertThat(html)
                .contains("Booking status updated")
                .contains("Nguyen Van A")
                .contains("#42")
                .contains("Meeting Room A")
                .contains("PENDING")
                .contains("APPROVED")
                .contains("250,000");
    }
}

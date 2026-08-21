package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ThymeleafEmailTemplateService implements EmailTemplateService {

    private static final String ACCOUNT_CONFIRMATION_TEMPLATE = "email/account-confirmation";
    private static final String PASSWORD_RESET_TEMPLATE = "email/password-reset";
    private static final String BOOKING_STATUS_CHANGED_TEMPLATE = "email/booking-status-changed";

    private final TemplateEngine templateEngine;
    private final AppOtpProperties otpProperties;

    @Override
    public String renderAccountConfirmation(String code, Locale locale) {
        return render(ACCOUNT_CONFIRMATION_TEMPLATE, code, locale);
    }

    @Override
    public String renderPasswordReset(String code, Locale locale) {
        return render(PASSWORD_RESET_TEMPLATE, code, locale);
    }

    @Override
    public String renderBookingStatusChanged(
            Booking booking, String previousStatus, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("booking", booking);
        context.setVariable("previousStatus", previousStatus);
        return templateEngine.process(BOOKING_STATUS_CHANGED_TEMPLATE, context);
    }

    private String render(String template, String code, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", otpProperties.getExpirationMinutes());
        return templateEngine.process(template, context);
    }
}

package com.nhom7.coworkingspace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

// Runs at the security filter chain level (before DispatcherServlet), so it - not
// GlobalExceptionHandler - is what actually produces the response body when a protected
// endpoint is rejected for missing/invalid/revoked credentials.
//
// Spring Security routes a rejected request to one of two different callbacks depending on
// whether the current principal is anonymous or not: a request with NO token at all (or one
// JwtAuthenticationFilter simply skipped) leaves the SecurityContext anonymous, which Spring
// Security treats as "not authenticated yet" -> AuthenticationEntryPoint.commence(). A request
// where something IS authenticated but forbidden (e.g. role checks) goes through
// AccessDeniedHandler.handle() instead. Both must be implemented here, otherwise one of the two
// paths silently falls back to Spring Boot's generic default error page.
@Component
@RequiredArgsConstructor
public class JwtAuthErrorHandler implements AccessDeniedHandler, AuthenticationEntryPoint {

    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        writeResponse(request, response);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        writeResponse(request, response);
    }

    private void writeResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.REJECTION_REASON_ATTRIBUTE);
        String messageKey;
        if (JwtAuthenticationFilter.REASON_REVOKED.equals(reason)) {
            messageKey = "auth.token.revoked";
        } else if (JwtAuthenticationFilter.REASON_INVALID.equals(reason)) {
            messageKey = "auth.token.invalid";
        } else {
            messageKey = "auth.token.missing";
        }

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(messageKey, null, locale);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(HttpStatus.FORBIDDEN.value(), message));
    }
}

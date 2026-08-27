package com.nhom7.coworkingspace.security;

import com.nhom7.coworkingspace.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    public static final String REJECTION_REASON_ATTRIBUTE =
            "jwtRejectionReason";

    public static final String REASON_REVOKED =
            "revoked";

    public static final String REASON_INVALID =
            "invalid";

    /**
     * Cookie chỉ phục vụ Web UI.
     *
     * /api/** không đọc cookie này.
     */
    public static final String MODERATOR_ACCESS_TOKEN_COOKIE =
            "moderator_access_token";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token =
                extractTokenFromRequest(
                        request
                );

        if (token != null) {

            if (
                    !jwtTokenProvider.validateToken(token)
                    || !jwtTokenProvider.isAccessToken(token)
            ) {

                request.setAttribute(
                        REJECTION_REASON_ATTRIBUTE,
                        REASON_INVALID
                );

            } else if (
                    tokenBlacklistService.isBlacklisted(
                            token
                    )
            ) {

                request.setAttribute(
                        REJECTION_REASON_ATTRIBUTE,
                        REASON_REVOKED
                );

            } else {

                String username =
                        jwtTokenProvider.extractUsername(
                                token
                        );

                Date issuedAt =
                        jwtTokenProvider.extractIssuedAt(
                                token
                        );

                if (
                        tokenBlacklistService.isUserTokenRevoked(
                                username,
                                issuedAt
                        )
                ) {

                    log.warn(
                            "[JWT Filter] Token was revoked for user: {}",
                            username
                    );

                    request.setAttribute(
                            REJECTION_REASON_ATTRIBUTE,
                            REASON_REVOKED
                    );

                } else {

                    UserDetails userDetails =
                            userDetailsService
                                    .loadUserByUsername(
                                            username
                                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );

                    log.debug(
                            "[JWT Filter] Authenticated - user: {}, uri: {}",
                            username,
                            request.getRequestURI()
                    );
                }
            }
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    /**
     * REST API:
     *
     * Authorization: Bearer <token>
     *
     * Web Management:
     *
     * moderator_access_token cookie
     *
     * API tuyệt đối không fallback sang cookie.
     */
    private String extractTokenFromRequest(
            HttpServletRequest request
    ) {

        String bearerToken =
                request.getHeader(
                        "Authorization"
                );

        if (
                StringUtils.hasText(
                        bearerToken
                )
                && bearerToken.startsWith(
                        "Bearer "
                )
        ) {

            return bearerToken.substring(7);
        }

        String requestUri =
                request.getRequestURI();

        /*
         * API vẫn chỉ sử dụng Bearer Token.
         */
        if (
                requestUri.startsWith(
                        "/api/"
                )
        ) {

            return null;
        }

        boolean managementWebRequest =
                requestUri.startsWith(
                        "/moderator/"
                )
                ||
                requestUri.startsWith(
                        "/admin/"
                );

        if (!managementWebRequest) {

            return null;
        }

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {

            return null;
        }

        for (Cookie cookie : cookies) {

            if (
                    MODERATOR_ACCESS_TOKEN_COOKIE.equals(
                            cookie.getName()
                    )
                    &&
                    StringUtils.hasText(
                            cookie.getValue()
                    )
            ) {

                return cookie.getValue();
            }
        }

        return null;
    }
}
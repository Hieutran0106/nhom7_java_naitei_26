package com.nhom7.coworkingspace.security;

import com.nhom7.coworkingspace.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Read by JwtAccessDeniedHandler to turn a bare 403 into a message that tells the caller
    // WHY (e.g. "you logged out" vs "please sign in") instead of a generic "Access Denied".
    public static final String REJECTION_REASON_ATTRIBUTE = "jwtRejectionReason";
    public static final String REASON_REVOKED = "revoked";
    public static final String REASON_INVALID = "invalid";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null) {
            if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
                request.setAttribute(REJECTION_REASON_ATTRIBUTE, REASON_INVALID);
            } else if (tokenBlacklistService.isBlacklisted(token)) {
                request.setAttribute(REJECTION_REASON_ATTRIBUTE, REASON_REVOKED);
            } else {
                String username = jwtTokenProvider.extractUsername(token);
                Date issuedAt = jwtTokenProvider.extractIssuedAt(token);
                if (tokenBlacklistService.isUserTokenRevoked(username, issuedAt)) {
                    log.warn("[JWT Filter] Token was revoked for user: {}", username);
                    request.setAttribute(REJECTION_REASON_ATTRIBUTE, REASON_REVOKED);
                } else {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credentials are not needed after JWT verification
                                    userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("[JWT Filter] Authenticated - user: {}, uri: {}",
                            username, request.getRequestURI());
                }
            }
        }

        // Must always be called to pass the request down the filter chain
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

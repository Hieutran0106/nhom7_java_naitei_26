package com.nhom7.coworkingspace.security;

import com.nhom7.coworkingspace.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey buildSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(buildSigningKey())
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(buildSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("[JWT] Token expired - subject: {}", ex.getClaims().getSubject(), ex);
        } catch (MalformedJwtException ex) {
            log.warn("[JWT] Malformed token: {}", ex.getMessage(), ex);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("[JWT] Invalid token: {}", ex.getMessage(), ex);
        }
        return false;
    }

    // Throws JwtException if the token is invalid, allowing validateToken() to catch it
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(buildSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

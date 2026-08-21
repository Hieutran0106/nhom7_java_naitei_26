package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String token, Date expiryDate) {
        if (token != null && expiryDate != null) {
            blacklist.put(token, expiryDate);
            log.info("[Blacklist] Token blacklisted until {}", expiryDate);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Date expiry = blacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.before(new Date())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    // Periodically remove expired tokens from blacklist every hour
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredTokens() {
        Date now = new Date();
        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
        log.debug("[Blacklist] Cleaned up expired blacklisted tokens");
    }
}

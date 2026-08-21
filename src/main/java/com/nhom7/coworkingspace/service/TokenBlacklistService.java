package com.nhom7.coworkingspace.service;

import java.util.Date;

public interface TokenBlacklistService {

    void blacklistToken(String token, Date expiryDate);

    boolean isBlacklisted(String token);

    void blacklistUserTokens(String email, Date issuedBefore);

    boolean isUserTokenRevoked(String email, Date issuedAt);
}

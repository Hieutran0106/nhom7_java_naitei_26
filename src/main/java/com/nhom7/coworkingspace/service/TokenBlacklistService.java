package com.nhom7.coworkingspace.service;

import java.util.Date;

public interface TokenBlacklistService {

    void blacklistToken(String token, Date expiryDate);

    boolean isBlacklisted(String token);
}

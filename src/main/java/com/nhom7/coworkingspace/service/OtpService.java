package com.nhom7.coworkingspace.service;

public interface OtpService {

    void sendConfirmationOtp(String email);

    void sendPasswordResetOtp(String email);

    void confirmAccount(String email, String otp);

    void resetPassword(String email, String otp, String newPassword);
}

package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.LoginRequest;
import com.nhom7.coworkingspace.dto.request.SignupRequest;
import com.nhom7.coworkingspace.dto.response.LoginResponse;
import com.nhom7.coworkingspace.dto.response.SignupResponse;

public interface AuthService {

    SignupResponse signup(SignupRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String authHeader);
}

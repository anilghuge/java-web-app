package com.banking.service;

import com.banking.dto.AuthDtos;

public interface AuthService {

    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request);

    AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);

    AuthDtos.AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    AuthDtos.ForgotPasswordResponse forgotPassword(String email);

    void resetPassword(AuthDtos.ResetPasswordRequest request);

    void changePassword(AuthDtos.ChangePasswordRequest request);

    AuthDtos.UserSummary me();
}

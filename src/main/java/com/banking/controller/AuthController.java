package com.banking.controller;

import com.banking.dto.AuthDtos;
import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.MessageResponse;
import com.banking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token rotation and password recovery")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a customer account",
            description = "Creates a CUSTOMER user and customer profile, then returns JWT tokens.")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain JWT access and refresh tokens")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate a refresh token and issue a new access token")
    public ApiResponse<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token (or all sessions when omitted)")
    public ApiResponse<MessageResponse> logout(@RequestBody(required = false) AuthDtos.LogoutRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ApiResponse.ok("Logged out successfully", new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Generate a password reset token",
            description = "In local development the token is returned when banking.security.expose-reset-token=true.")
    public ApiResponse<AuthDtos.ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        return ApiResponse.ok("Password reset request processed", authService.forgotPassword(request.email()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password using a one-time token")
    public ApiResponse<MessageResponse> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset successful", new MessageResponse("Password reset successful"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the authenticated user's profile summary")
    public ApiResponse<AuthDtos.UserSummary> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change the authenticated user's password")
    public ApiResponse<MessageResponse> changePassword(
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok("Password changed successfully. Please sign in again.",
                new MessageResponse("Password changed successfully"));
    }
}

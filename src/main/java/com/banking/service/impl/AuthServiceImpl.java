package com.banking.service.impl;

import com.banking.dto.AuthDtos;
import com.banking.entity.Customer;
import com.banking.entity.Employee;
import com.banking.entity.PasswordResetToken;
import com.banking.entity.RefreshToken;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.enums.KycStatus;
import com.banking.enums.NotificationType;
import com.banking.enums.RoleName;
import com.banking.exception.ApiException;
import com.banking.repository.CustomerRepository;
import com.banking.repository.EmployeeRepository;
import com.banking.repository.PasswordResetTokenRepository;
import com.banking.repository.RefreshTokenRepository;
import com.banking.repository.RoleRepository;
import com.banking.repository.UserRepository;
import com.banking.security.JwtService;
import com.banking.service.AuditService;
import com.banking.service.AuthService;
import com.banking.service.NotificationService;
import com.banking.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${banking.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    @Value("${banking.security.expose-reset-token:false}")
    private boolean exposeResetToken;

    @Override
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_EXISTS", "An account already exists with this email");
        }
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> ApiException.badRequest("ROLE_NOT_CONFIGURED", "Customer role is not configured"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.addRole(customerRole);
        userRepository.save(user);

        Customer customer = new Customer();
        customer.setCustomerNumber(generateUniqueCustomerNumber());
        customer.setUser(user);
        customer.setKycStatus(KycStatus.PENDING);
        customer.setCountry("India");
        customerRepository.save(customer);

        notificationService.notify(user, "Welcome to Northstar Bank",
                "Your digital banking profile is ready. Complete KYC and open your first account.",
                NotificationType.SYSTEM, "/profile");
        auditService.logAs(email, "REGISTER", "Customer", customer.getId(), "Customer self-registration", "SUCCESS");

        return issueTokens(user, false);
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException ex) {
            auditService.logAs(email, "LOGIN_FAILED", "User", null, "Invalid credentials", "FAILED");
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password"));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditService.logAs(email, "LOGIN", "User", user.getId(), "Successful login", "SUCCESS");
        return issueTokens(user, request.rememberMe());
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw ApiException.badRequest("REFRESH_TOKEN_REQUIRED", "Refresh token is required");
        }
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), Instant.now());
            throw ApiException.unauthorized("REFRESH_TOKEN_REUSED",
                    "Refresh token has already been used. All sessions were revoked for security.");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }
        if (!stored.getUser().isEnabled()) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "This user account is disabled");
        }

        User user = stored.getUser();
        RefreshToken replacement = buildRefreshToken(user, refreshTokenDays);
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        stored.setReplacedBy(replacement.getToken());
        refreshTokenRepository.save(stored);
        refreshTokenRepository.save(replacement);

        String accessToken = jwtService.generateAccessToken(user);
        auditService.logAs(user.getEmail(), "TOKEN_REFRESH", "User", user.getId(),
                "Refresh token rotated", "SUCCESS");
        return new AuthDtos.AuthResponse(accessToken, replacement.getToken(), "Bearer",
                jwtService.getAccessTokenSeconds(), buildUserSummary(user));
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
        String email = com.banking.security.SecurityUtils.currentEmail();
        if (email != null) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
                    refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
                }
                auditService.logAs(email, "LOGOUT", "User", user.getId(), "User logged out", "SUCCESS");
            });
        }
    }

    @Override
    @Transactional
    public AuthDtos.ForgotPasswordResponse forgotPassword(String emailValue) {
        String email = emailValue.trim().toLowerCase();
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        String rawToken = null;
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            rawToken = generateResetToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(hash(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);
            log.info("Password reset token generated for {}: {}", email, rawToken);
            auditService.logAs(email, "PASSWORD_RESET_REQUESTED", "User", user.getId(),
                    "Password reset requested", "SUCCESS");
        }
        String message = "If an account exists for this email, a password reset link has been generated.";
        return new AuthDtos.ForgotPasswordResponse(message, exposeResetToken ? rawToken : null);
    }

    @Override
    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw ApiException.badRequest("PASSWORD_MISMATCH", "New password and confirmation do not match");
        }
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hash(request.token()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_RESET_TOKEN", "Reset token is invalid or already used"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("RESET_TOKEN_EXPIRED", "Reset token has expired");
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        auditService.logAs(user.getEmail(), "PASSWORD_RESET", "User", user.getId(),
                "Password reset completed", "SUCCESS");
        notificationService.notify(user, "Password changed",
                "Your password was reset successfully. If this wasn't you, contact support immediately.",
                NotificationType.SECURITY, "/profile");
    }

    @Override
    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw ApiException.badRequest("PASSWORD_REUSED", "New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        auditService.logAs(user.getEmail(), "PASSWORD_CHANGED", "User", user.getId(),
                "Password changed by user", "SUCCESS");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.UserSummary me() {
        return buildUserSummary(currentUser());
    }

    private AuthDtos.AuthResponse issueTokens(User user, boolean rememberMe) {
        long days = rememberMe ? Math.max(refreshTokenDays, 30) : refreshTokenDays;
        RefreshToken refreshToken = buildRefreshToken(user, days);
        refreshTokenRepository.save(refreshToken);
        String accessToken = jwtService.generateAccessToken(user);
        return new AuthDtos.AuthResponse(accessToken, refreshToken.getToken(), "Bearer",
                jwtService.getAccessTokenSeconds(), buildUserSummary(user));
    }

    private RefreshToken buildRefreshToken(User user, long days) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(ReferenceGenerator.refreshToken());
        refreshToken.setExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS));
        return refreshToken;
    }

    private AuthDtos.UserSummary buildUserSummary(User user) {
        Optional<Customer> customer = customerRepository.findByUserId(user.getId());
        Optional<Employee> employee = employeeRepository.findByUserId(user.getId());
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .sorted()
                .toList();
        return new AuthDtos.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                customer.map(Customer::getId).orElse(null),
                employee.map(Employee::getId).orElse(null),
                employee.map(e -> e.getBranch().getName()).orElse(null),
                customer.map(c -> c.getKycStatus().name()).orElse(null),
                user.getPhone());
    }

    private User currentUser() {
        String email = com.banking.security.SecurityUtils.currentEmail();
        if (email == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Authentication is required");
        }
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("USER_NOT_FOUND", "User no longer exists"));
    }

    private String generateUniqueCustomerNumber() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String number = ReferenceGenerator.customerNumber();
            if (!customerRepository.existsByCustomerNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to generate a unique customer number");
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}

package com.banking.dto;

import com.banking.enums.Gender;
import com.banking.enums.KycStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CreateCustomerRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 20) String phone,
            LocalDate dateOfBirth,
            Gender gender,
            @Size(max = 255) String address,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 12) String postalCode,
            @Size(max = 30) String nationalId,
            @Size(max = 100) String occupation,
            @DecimalMin("0.0") BigDecimal annualIncome) {
    }

    public record UpdateCustomerRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Size(max = 20) String phone,
            LocalDate dateOfBirth,
            Gender gender,
            @Size(max = 255) String address,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @Size(max = 12) String postalCode,
            @Size(max = 30) String nationalId,
            @Size(max = 100) String occupation,
            @DecimalMin("0.0") BigDecimal annualIncome) {
    }

    public record KycSubmissionRequest(
            @NotBlank @Size(max = 30) String nationalId,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 12) String postalCode,
            @Size(max = 100) String occupation,
            @DecimalMin("0.0") BigDecimal annualIncome) {
    }

    public record KycDecisionRequest(
            @NotNull KycStatus status,
            @Size(max = 255) String remarks) {
    }

    public record CustomerResponse(
            Long id,
            String customerNumber,
            Long userId,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            Gender gender,
            String address,
            String city,
            String state,
            String postalCode,
            String country,
            String nationalIdMasked,
            String occupation,
            BigDecimal annualIncome,
            KycStatus kycStatus,
            Instant kycVerifiedAt,
            long accountCount,
            BigDecimal totalBalance,
            Instant createdAt) {
    }

    public record CustomerSummary(
            Long id,
            String customerNumber,
            String fullName,
            String email,
            String phone,
            KycStatus kycStatus,
            Instant createdAt) {
    }

    public record CustomerFilter(
            String query,
            KycStatus kycStatus,
            String city,
            int page,
            int size,
            String sortBy,
            String sortDir) {
    }

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
    public record Ifsc(String value) {
    }
}

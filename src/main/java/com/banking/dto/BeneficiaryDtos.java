package com.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class BeneficiaryDtos {

    private BeneficiaryDtos() {
    }

    public record BeneficiaryRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(min = 8, max = 20) String accountNumber,
            @NotBlank @Size(max = 120) String bankName,
            @Size(max = 120) String branchName,
            @NotBlank @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code") String ifscCode,
            @Size(max = 60) String nickname,
            @DecimalMin("1.0") BigDecimal dailyLimit) {
    }

    public record BeneficiaryResponse(
            Long id,
            String name,
            String accountNumber,
            String bankName,
            String branchName,
            String ifscCode,
            String nickname,
            BigDecimal dailyLimit,
            boolean active,
            Instant createdAt) {
    }
}

package com.banking.dto;

import com.banking.enums.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record OpenAccountRequest(
            Long customerId,
            @NotBlank String accountTypeCode,
            @NotBlank String branchCode,
            @Size(max = 60) String nickname,
            @DecimalMin(value = "0.0", inclusive = true) BigDecimal initialDeposit) {
    }

    public record UpdateAccountStatusRequest(@NotNull AccountStatus status) {
    }

    public record AccountResponse(
            Long id,
            String accountNumber,
            Long customerId,
            String customerName,
            String customerNumber,
            String accountTypeCode,
            String accountTypeName,
            String branchCode,
            String branchName,
            String ifscCode,
            BigDecimal balance,
            BigDecimal availableBalance,
            String currency,
            AccountStatus status,
            String nickname,
            BigDecimal interestRate,
            Instant openedAt) {
    }

    public record AccountSummary(
            Long id,
            String accountNumber,
            String accountTypeName,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            String nickname) {
    }

    public record AccountBalanceResponse(
            String accountNumber,
            BigDecimal balance,
            BigDecimal availableBalance,
            String currency,
            AccountStatus status) {
    }

    public record AccountTypeResponse(
            Long id,
            String code,
            String name,
            String description,
            BigDecimal interestRate,
            BigDecimal minimumBalance,
            BigDecimal dailyWithdrawalLimit) {
    }

    public record BranchOption(
            Long id,
            String code,
            String name,
            String city,
            String ifscCode) {
    }
}

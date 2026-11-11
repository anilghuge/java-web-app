package com.banking.dto;

import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record DepositRequest(
            @NotBlank String accountNumber,
            @NotNull @Positive @Digits(integer = 17, fraction = 2)
            @DecimalMin(value = "1.00", message = "Minimum deposit is 1.00") BigDecimal amount,
            @Size(max = 255) String description) {
    }

    public record WithdrawRequest(
            @NotBlank String accountNumber,
            @NotNull @Positive @Digits(integer = 17, fraction = 2)
            @DecimalMin(value = "1.00", message = "Minimum withdrawal is 1.00") BigDecimal amount,
            @Size(max = 255) String description) {
    }

    public record TransferRequest(
            @NotBlank String fromAccountNumber,
            @NotBlank String toAccountNumber,
            @NotNull @Positive @Digits(integer = 17, fraction = 2)
            @DecimalMin(value = "1.00", message = "Minimum transfer is 1.00") BigDecimal amount,
            @Size(max = 255) String description,
            String idempotencyKey) {
    }

    public record TransactionResponse(
            Long id,
            String reference,
            String transactionGroup,
            Long accountId,
            String accountNumber,
            String customerName,
            String counterpartyAccountNumber,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            String performedBy,
            Instant createdAt) {
    }

    public record TransferResponse(
            String transactionGroup,
            String reference,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            BigDecimal fromBalance,
            BigDecimal toBalance,
            TransactionResponse debitTransaction,
            TransactionResponse creditTransaction,
            boolean idempotentReplay) {
    }

    public record StatementResponse(
            String accountNumber,
            String accountHolder,
            String bankName,
            Instant from,
            Instant to,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal totalCredits,
            BigDecimal totalDebits,
            List<TransactionResponse> transactions) {
    }

    public record TransactionFilter(
            Long accountId,
            Long customerId,
            TransactionType type,
            TransactionStatus status,
            Instant from,
            Instant to,
            String query,
            int page,
            int size,
            String sortBy,
            String sortDir) {
    }
}

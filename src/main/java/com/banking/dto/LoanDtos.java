package com.banking.dto;

import com.banking.enums.LoanPaymentStatus;
import com.banking.enums.LoanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class LoanDtos {

    private LoanDtos() {
    }

    public record LoanApplicationRequest(
            @NotBlank String loanType,
            @NotNull @Positive @DecimalMin("1000.00") @Digits(integer = 17, fraction = 2) BigDecimal principal,
            @NotNull @Min(6) @Max(360) Integer termMonths,
            @NotBlank @Size(max = 255) String purpose,
            @NotBlank String disbursementAccountNumber) {
    }

    public record LoanDecisionRequest(@Size(max = 255) String remarks) {
    }

    public record LoanRejectionRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record LoanPaymentRequest(
            @NotBlank String accountNumber,
            @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
    }

    public record EmiCalculationRequest(
            @NotNull @Positive BigDecimal principal,
            @NotNull @Positive BigDecimal annualInterestRate,
            @NotNull @Min(1) @Max(360) Integer termMonths) {
    }

    public record EmiCalculationResponse(
            BigDecimal emi,
            BigDecimal totalPayable,
            BigDecimal totalInterest) {
    }

    public record LoanResponse(
            Long id,
            String loanNumber,
            Long customerId,
            String customerName,
            String disbursementAccountNumber,
            String loanType,
            BigDecimal principal,
            BigDecimal annualInterestRate,
            int termMonths,
            BigDecimal emi,
            BigDecimal totalPayable,
            BigDecimal outstandingBalance,
            LoanStatus status,
            String purpose,
            Instant appliedAt,
            Instant approvedAt,
            String approvedBy,
            String rejectionReason,
            LocalDate nextDueDate,
            int paidInstallments,
            long remainingInstallments,
            Instant createdAt) {
    }

    public record LoanScheduleItem(
            Long id,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal amount,
            BigDecimal principalComponent,
            BigDecimal interestComponent,
            LoanPaymentStatus status,
            Instant paidAt,
            String transactionReference) {
    }

    public record LoanScheduleResponse(
            String loanNumber,
            BigDecimal emi,
            int termMonths,
            int paidInstallments,
            BigDecimal outstandingBalance,
            List<LoanScheduleItem> installments) {
    }

    public record LoanPaymentResponse(
            String loanNumber,
            String transactionReference,
            BigDecimal amount,
            BigDecimal outstandingBalance,
            int paidInstallments,
            LoanPaymentStatus installmentStatus,
            Instant paidAt) {
    }
}

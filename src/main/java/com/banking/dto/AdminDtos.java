package com.banking.dto;

import com.banking.enums.RoleName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record BranchRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 12) String postalCode,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code") String ifscCode,
            boolean active) {
    }

    public record BranchResponse(
            Long id,
            String code,
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            String phone,
            String email,
            String ifscCode,
            boolean active,
            int employeeCount,
            int accountCount,
            Instant createdAt) {
    }

    public record EmployeeRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 160) String email,
            @Size(min = 8, max = 72) String password,
            @Size(max = 20) String phone,
            @NotNull Long branchId,
            @NotBlank @Size(max = 80) String designation,
            @NotNull LocalDate joiningDate,
            @DecimalMin("0.0") BigDecimal salary,
            @NotNull RoleName role) {
    }

    public record EmployeeResponse(
            Long id,
            String employeeCode,
            Long userId,
            String firstName,
            String lastName,
            String email,
            String phone,
            Long branchId,
            String branchName,
            String branchCode,
            String designation,
            LocalDate joiningDate,
            BigDecimal salary,
            boolean active,
            List<String> roles,
            Instant createdAt) {
    }

    public record PermissionResponse(
            Long id,
            String name,
            String description,
            String module) {
    }

    public record RoleResponse(
            Long id,
            String name,
            String description,
            List<PermissionResponse> permissions,
            long userCount) {
    }

    public record NotificationResponse(
            Long id,
            String title,
            String message,
            String type,
            boolean read,
            String link,
            Instant createdAt) {
    }

    public record AuditLogResponse(
            Long id,
            String userEmail,
            String action,
            String entityName,
            String entityId,
            String details,
            String ipAddress,
            String status,
            Instant createdAt) {
    }

    public record ReportSummary(
            Instant from,
            Instant to,
            BigDecimal totalDeposits,
            BigDecimal totalWithdrawals,
            BigDecimal totalTransfers,
            BigDecimal loanDisbursements,
            BigDecimal loanRepayments,
            BigDecimal averageBalance,
            long newCustomers,
            long newAccounts,
            long transactionCount,
            long pendingLoans,
            List<CommonDtos.NameValue> transactionMix) {
    }

    public record CustomerGrowthPoint(String month, long customers, long accounts) {
    }

    public record ReportDashboard(
            ReportSummary summary,
            List<CustomerGrowthPoint> growth,
            List<DashboardDtos.TransactionAnalyticsPoint> transactionTrend) {
    }
}

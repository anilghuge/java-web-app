package com.banking.dto;

import com.banking.enums.AccountStatus;
import com.banking.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSummary(
            String role,
            // Staff metrics
            long totalCustomers,
            long totalAccounts,
            BigDecimal totalBalance,
            BigDecimal totalDeposits,
            BigDecimal totalWithdrawals,
            BigDecimal totalTransfers,
            long activeLoans,
            long pendingLoans,
            long totalCards,
            long blockedCards,
            long todayTransactions,
            BigDecimal monthlyTransactionVolume,
            long unreadNotifications,
            // Customer metrics
            List<AccountDtos.AccountSummary> myAccounts,
            BigDecimal myTotalBalance,
            BigDecimal mySavingsBalance,
            BigDecimal myCurrentBalance,
            List<TransactionDtos.TransactionResponse> myRecentTransactions,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            String kycStatus,
            long myActiveLoans,
            long myUnreadNotifications) {
    }

    public record TransactionAnalyticsPoint(
            String label,
            BigDecimal income,
            BigDecimal expenses,
            long transactionCount) {
    }

    public record MonthlyRevenuePoint(
            String month,
            BigDecimal deposits,
            BigDecimal withdrawals,
            BigDecimal transfers,
            BigDecimal revenue) {
    }

    public record AccountStatistics(
            long totalAccounts,
            long activeAccounts,
            long dormantAccounts,
            long frozenAccounts,
            long closedAccounts,
            List<TypeStat> byType,
            List<StatusStat> byStatus) {
    }

    public record TypeStat(String code, long accounts, BigDecimal balance) {
    }

    public record StatusStat(AccountStatus status, long count) {
    }

    public record RecentActivity(
            String type,
            String title,
            String description,
            BigDecimal amount,
            String reference,
            java.time.Instant createdAt,
            String status) {
    }

    public record DashboardTransactions(
            List<TransactionDtos.TransactionResponse> recent,
            Map<TransactionType, BigDecimal> totalsByType) {
    }
}

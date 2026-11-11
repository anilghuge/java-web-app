package com.banking.controller;

import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.DashboardDtos;
import com.banking.dto.TransactionDtos;
import com.banking.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Role-aware banking analytics and dashboard metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get role-aware dashboard summary metrics")
    public ApiResponse<DashboardDtos.DashboardSummary> summary() {
        return ApiResponse.ok(dashboardService.summary());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get recent transactions (own for customers, all for staff)")
    public ApiResponse<PageResponse<TransactionDtos.TransactionResponse>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(dashboardService.transactions(page, size));
    }

    @GetMapping("/account-statistics")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "Get bank-wide account statistics (staff only)")
    public ApiResponse<DashboardDtos.AccountStatistics> accountStatistics() {
        return ApiResponse.ok(dashboardService.accountStatistics());
    }

    @GetMapping("/monthly-revenue")
    @Operation(summary = "Get monthly deposits, withdrawals and transfers for charts")
    public ApiResponse<List<DashboardDtos.MonthlyRevenuePoint>> monthlyRevenue(
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(dashboardService.monthlyRevenue(months));
    }

    @GetMapping("/transaction-trend")
    @Operation(summary = "Get monthly income versus expense analytics")
    public ApiResponse<List<DashboardDtos.TransactionAnalyticsPoint>> transactionTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(dashboardService.transactionTrend(months));
    }

    @GetMapping("/snapshot")
    @Operation(summary = "Get recent activity and month-to-date totals by transaction type")
    public ApiResponse<DashboardDtos.DashboardTransactions> snapshot() {
        return ApiResponse.ok(dashboardService.transactionSnapshot());
    }
}

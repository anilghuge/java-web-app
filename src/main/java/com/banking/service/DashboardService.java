package com.banking.service;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.DashboardDtos;
import com.banking.dto.TransactionDtos;

import java.util.List;

public interface DashboardService {

    DashboardDtos.DashboardSummary summary();

    PageResponse<TransactionDtos.TransactionResponse> transactions(int page, int size);

    DashboardDtos.AccountStatistics accountStatistics();

    List<DashboardDtos.MonthlyRevenuePoint> monthlyRevenue(int months);

    List<DashboardDtos.TransactionAnalyticsPoint> transactionTrend(int months);

    DashboardDtos.DashboardTransactions transactionSnapshot();
}

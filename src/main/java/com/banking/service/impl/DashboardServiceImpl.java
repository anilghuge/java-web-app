package com.banking.service.impl;

import com.banking.dto.AccountDtos;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.DashboardDtos;
import com.banking.dto.TransactionDtos;
import com.banking.entity.Account;
import com.banking.entity.Customer;
import com.banking.entity.Transaction;
import com.banking.enums.AccountStatus;
import com.banking.enums.LoanStatus;
import com.banking.enums.TransactionType;
import com.banking.mapper.AccountMapper;
import com.banking.mapper.TransactionMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.CardRepository;
import com.banking.repository.CustomerRepository;
import com.banking.repository.LoanRepository;
import com.banking.repository.NotificationRepository;
import com.banking.repository.TransactionRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.DashboardService;
import com.banking.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final CardRepository cardRepository;
    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional(readOnly = true)
    public DashboardDtos.DashboardSummary summary() {
        var user = authenticatedUserService.user();
        boolean staff = SecurityUtils.isStaff();
        Instant now = Instant.now();
        Instant startOfMonth = startOfMonth();
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);

        long unread = notificationRepository.countByUserIdAndReadFalse(user.getId());
        long totalCustomers = customerRepository.countByDeletedFalse();
        long totalAccounts = accountRepository.countByDeletedFalse();
        BigDecimal totalBalance = safe(accountRepository.sumTotalBalance());
        BigDecimal totalDeposits = safe(transactionRepository.sumByTypeAndPeriod(
                TransactionType.DEPOSIT, Instant.EPOCH, now));
        BigDecimal totalWithdrawals = safe(transactionRepository.sumByTypeAndPeriod(
                TransactionType.WITHDRAWAL, Instant.EPOCH, now));
        BigDecimal totalTransfers = safe(transactionRepository.sumByTypeAndPeriod(
                TransactionType.TRANSFER_DEBIT, Instant.EPOCH, now));
        long todayTransactions = transactionRepository.countByCreatedAtBetween(startOfDay, now);
        BigDecimal monthlyVolume = safe(transactionRepository.sumByTypeAndPeriod(
                        TransactionType.DEPOSIT, startOfMonth, now))
                .add(safe(transactionRepository.sumByTypeAndPeriod(
                        TransactionType.WITHDRAWAL, startOfMonth, now)))
                .add(safe(transactionRepository.sumByTypeAndPeriod(
                        TransactionType.TRANSFER_DEBIT, startOfMonth, now)));

        List<AccountDtos.AccountSummary> myAccounts = List.of();
        BigDecimal myTotal = BigDecimal.ZERO;
        BigDecimal mySavings = BigDecimal.ZERO;
        BigDecimal myCurrent = BigDecimal.ZERO;
        List<TransactionDtos.TransactionResponse> myRecent = List.of();
        BigDecimal monthlyIncome = BigDecimal.ZERO;
        BigDecimal monthlyExpenses = BigDecimal.ZERO;
        String kycStatus = null;
        long myActiveLoans = 0;
        long myUnread = unread;

        if (!staff) {
            Customer customer = authenticatedUserService.customer();
            List<Account> accounts = accountRepository
                    .findByCustomerIdAndDeletedFalseOrderByOpenedAtDesc(customer.getId());
            myAccounts = accounts.stream().map(AccountMapper::toSummary).toList();
            myTotal = safe(accountRepository.sumBalanceByCustomer(customer.getId()));
            mySavings = safe(accountRepository.sumBalanceByCustomerAndType(customer.getId(), "SAVINGS"));
            myCurrent = safe(accountRepository.sumBalanceByCustomerAndType(customer.getId(), "CURRENT"));
            myRecent = transactionRepository
                    .findTop8ByAccountCustomerUserIdOrderByCreatedAtDesc(customer.getId())
                    .stream().map(TransactionMapper::toResponse).toList();
            monthlyIncome = safe(transactionRepository.sumByCustomerAndTypeAndPeriod(
                            customer.getId(), TransactionType.DEPOSIT, startOfMonth, now))
                    .add(safe(transactionRepository.sumByCustomerAndTypeAndPeriod(
                            customer.getId(), TransactionType.TRANSFER_CREDIT, startOfMonth, now)))
                    .add(safe(transactionRepository.sumByCustomerAndTypeAndPeriod(
                            customer.getId(), TransactionType.LOAN_DISBURSEMENT, startOfMonth, now)));
            monthlyExpenses = safe(transactionRepository.sumByCustomerAndTypeAndPeriod(
                            customer.getId(), TransactionType.WITHDRAWAL, startOfMonth, now))
                    .add(safe(transactionRepository.sumByCustomerAndTypeAndPeriod(
                            customer.getId(), TransactionType.TRANSFER_DEBIT, startOfMonth, now)))
                    .add(safe(transactionRepository.sumByCustomerAndTypeAndPeriod(
                            customer.getId(), TransactionType.LOAN_REPAYMENT, startOfMonth, now)));
            kycStatus = customer.getKycStatus().name();
            myActiveLoans = loanRepository.countByCustomerIdAndStatus(customer.getId(), LoanStatus.ACTIVE);
        }

        String role = SecurityUtils.roles().stream()
                .sorted((a, b) -> Integer.compare(roleRank(a), roleRank(b)))
                .findFirst().orElse("CUSTOMER");

        return new DashboardDtos.DashboardSummary(
                role,
                totalCustomers, totalAccounts, totalBalance, totalDeposits, totalWithdrawals,
                totalTransfers, loanRepository.countActiveLoans(),
                loanRepository.countByStatus(LoanStatus.PENDING),
                cardRepository.count(), cardRepository.countByStatus(com.banking.enums.CardStatus.BLOCKED),
                todayTransactions, monthlyVolume, unread,
                myAccounts, myTotal, mySavings, myCurrent, myRecent,
                monthlyIncome, monthlyExpenses, kycStatus, myActiveLoans, myUnread);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDtos.TransactionResponse> transactions(int page, int size) {
        Pageable pageable = PaginationUtil.pageable(page, size, "createdAt", "desc");
        Page<Transaction> transactions;
        if (SecurityUtils.isStaff()) {
            transactions = transactionRepository.findAll(pageable);
        } else {
            transactions = transactionRepository.findByAccountCustomerUserId(
                    authenticatedUserService.customer().getId(), pageable);
        }
        return new PageResponse<>(
                transactions.getContent().stream().map(TransactionMapper::toResponse).toList(),
                transactions.getNumber(), transactions.getSize(), transactions.getTotalElements(),
                transactions.getTotalPages(), transactions.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDtos.AccountStatistics accountStatistics() {
        long total = accountRepository.countByDeletedFalse();
        long active = accountRepository.countByStatusAndDeletedFalse(AccountStatus.ACTIVE);
        long dormant = accountRepository.countByStatusAndDeletedFalse(AccountStatus.DORMANT);
        long frozen = accountRepository.countByStatusAndDeletedFalse(AccountStatus.FROZEN);
        long closed = accountRepository.countByStatusAndDeletedFalse(AccountStatus.CLOSED);
        List<DashboardDtos.TypeStat> byType = accountRepository.statisticsByType().stream()
                .map(row -> new DashboardDtos.TypeStat(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]))
                .toList();
        List<DashboardDtos.StatusStat> byStatus = List.of(
                new DashboardDtos.StatusStat(AccountStatus.ACTIVE, active),
                new DashboardDtos.StatusStat(AccountStatus.DORMANT, dormant),
                new DashboardDtos.StatusStat(AccountStatus.FROZEN, frozen),
                new DashboardDtos.StatusStat(AccountStatus.CLOSED, closed));
        return new DashboardDtos.AccountStatistics(total, active, dormant, frozen, closed, byType, byStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardDtos.MonthlyRevenuePoint> monthlyRevenue(int months) {
        int safeMonths = Math.min(Math.max(months, 1), 24);
        YearMonth start = YearMonth.now(ZoneOffset.UTC).minusMonths(safeMonths - 1L);
        Instant from = start.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Transaction> transactions = transactionsForRange(from, Instant.now());

        Map<YearMonth, DashboardDtos.MonthlyRevenuePoint> points = new LinkedHashMap<>();
        for (int i = 0; i < safeMonths; i++) {
            YearMonth month = start.plusMonths(i);
            points.put(month, new DashboardDtos.MonthlyRevenuePoint(
                    month.atDay(1).format(MONTH_LABEL), BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO));
        }
        for (Transaction transaction : transactions) {
            YearMonth month = YearMonth.from(transaction.getCreatedAt().atZone(ZoneOffset.UTC));
            DashboardDtos.MonthlyRevenuePoint current = points.get(month);
            if (current == null) {
                continue;
            }
            BigDecimal deposits = current.deposits();
            BigDecimal withdrawals = current.withdrawals();
            BigDecimal transfers = current.transfers();
            switch (transaction.getType()) {
                case DEPOSIT -> deposits = deposits.add(transaction.getAmount());
                case WITHDRAWAL -> withdrawals = withdrawals.add(transaction.getAmount());
                case TRANSFER_DEBIT -> transfers = transfers.add(transaction.getAmount());
                default -> {
                }
            }
            points.put(month, new DashboardDtos.MonthlyRevenuePoint(
                    current.month(), deposits, withdrawals, transfers, deposits.subtract(withdrawals)));
        }
        return new ArrayList<>(points.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardDtos.TransactionAnalyticsPoint> transactionTrend(int months) {
        int safeMonths = Math.min(Math.max(months, 1), 24);
        YearMonth start = YearMonth.now(ZoneOffset.UTC).minusMonths(safeMonths - 1L);
        Instant from = start.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Transaction> transactions = transactionsForRange(from, Instant.now());

        Map<YearMonth, BigDecimal[]> amounts = new LinkedHashMap<>();
        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < safeMonths; i++) {
            YearMonth month = start.plusMonths(i);
            amounts.put(month, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            counts.put(month, 0L);
        }
        for (Transaction transaction : transactions) {
            YearMonth month = YearMonth.from(transaction.getCreatedAt().atZone(ZoneOffset.UTC));
            BigDecimal[] bucket = amounts.get(month);
            if (bucket == null) {
                continue;
            }
            boolean credit = switch (transaction.getType()) {
                case DEPOSIT, TRANSFER_CREDIT, LOAN_DISBURSEMENT -> true;
                default -> false;
            };
            if (credit) {
                bucket[0] = bucket[0].add(transaction.getAmount());
            } else {
                bucket[1] = bucket[1].add(transaction.getAmount());
            }
            counts.merge(month, 1L, Long::sum);
        }
        List<DashboardDtos.TransactionAnalyticsPoint> result = new ArrayList<>();
        amounts.forEach((month, bucket) -> result.add(new DashboardDtos.TransactionAnalyticsPoint(
                month.atDay(1).format(MONTH_LABEL), bucket[0], bucket[1], counts.getOrDefault(month, 0L))));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDtos.DashboardTransactions transactionSnapshot() {
        Long customerId = SecurityUtils.isStaff() ? null : authenticatedUserService.customer().getId();
        Instant from = startOfMonth();
        List<Transaction> transactions = customerId == null
                ? transactionRepository.findByCreatedAtBetween(from, Instant.now())
                : transactionRepository.findByAccountCustomerUserId(customerId,
                org.springframework.data.domain.PageRequest.of(0, 1000,
                        org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();

        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        for (Transaction transaction : transactions) {
            if (transaction.getCreatedAt().isBefore(from)) {
                continue;
            }
            totals.merge(transaction.getType(), transaction.getAmount(), BigDecimal::add);
        }
        List<TransactionDtos.TransactionResponse> recent = (customerId == null
                ? transactionRepository.findTop10ByOrderByCreatedAtDesc()
                : transactionRepository.findTop8ByAccountCustomerUserIdOrderByCreatedAtDesc(customerId))
                .stream().map(TransactionMapper::toResponse).toList();
        return new DashboardDtos.DashboardTransactions(recent, totals);
    }

    private List<Transaction> transactionsForRange(Instant from, Instant to) {
        if (SecurityUtils.isStaff()) {
            return transactionRepository.findByCreatedAtBetween(from, to);
        }
        Long customerId = authenticatedUserService.customer().getId();
        return transactionRepository.findByAccountCustomerUserId(customerId,
                        org.springframework.data.domain.PageRequest.of(0, 5000,
                                org.springframework.data.domain.Sort.by("createdAt").descending()))
                .getContent().stream()
                .filter(t -> !t.getCreatedAt().isBefore(from) && !t.getCreatedAt().isAfter(to))
                .toList();
    }

    private Instant startOfMonth() {
        return YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int roleRank(String role) {
        return switch (role) {
            case "ADMIN" -> 0;
            case "MANAGER" -> 1;
            case "BANK_EMPLOYEE" -> 2;
            default -> 3;
        };
    }
}

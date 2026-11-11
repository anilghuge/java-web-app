package com.banking.service.impl;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.TransactionDtos;
import com.banking.entity.Account;
import com.banking.entity.IdempotencyRecord;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Transaction;
import com.banking.enums.AccountStatus;
import com.banking.enums.IdempotencyStatus;
import com.banking.enums.LedgerEntryType;
import com.banking.enums.NotificationType;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exception.ApiException;
import com.banking.mapper.TransactionMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.IdempotencyRecordRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.TransactionRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AuditService;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.NotificationService;
import com.banking.service.TransactionService;
import com.banking.util.PaginationUtil;
import com.banking.util.ReferenceGenerator;
import com.banking.util.StatementPdfGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final Set<TransactionType> CREDIT_TYPES = Set.of(
            TransactionType.DEPOSIT, TransactionType.TRANSFER_CREDIT,
            TransactionType.LOAN_DISBURSEMENT, TransactionType.CARD_PAYMENT);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AuthenticatedUserService authenticatedUserService;
    private final StatementPdfGenerator statementPdfGenerator;

    @Value("${banking.withdrawal.max-per-transaction:100000}")
    private BigDecimal maxWithdrawalPerTransaction;

    @Value("${banking.withdrawal.max-daily:250000}")
    private BigDecimal maxDailyWithdrawal;

    @Override
    @Transactional
    public TransactionDtos.TransactionResponse deposit(TransactionDtos.DepositRequest request) {
        Account account = lockAccount(request.accountNumber());
        assertAccountAccess(account);
        ensureActive(account);
        BigDecimal amount = normalize(request.amount());

        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        account.credit(amount);
        accountRepository.save(account);

        Transaction transaction = buildTransaction(account, null, TransactionType.DEPOSIT,
                amount, account.getBalance(), "Cash deposit" + descriptionSuffix(request.description()),
                group, reference);
        transactionRepository.save(transaction);
        writeLedger(group, reference, "CASH", LedgerEntryType.DEBIT, amount, "Cash received");
        writeLedger(group, reference, account.getAccountNumber(), LedgerEntryType.CREDIT, amount, "Deposit credited");

        notificationService.notify(account.getCustomer().getUser(), "Deposit received",
                format(amount) + " was deposited to account " + account.getAccountNumber() + ".",
                NotificationType.TRANSACTION, "/transactions");
        auditService.log("DEPOSIT", "Account", account.getId(),
                "Deposit " + format(amount) + " reference " + reference);
        return TransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionDtos.TransactionResponse withdraw(TransactionDtos.WithdrawRequest request) {
        Account account = lockAccount(request.accountNumber());
        assertAccountAccess(account);
        ensureActive(account);
        BigDecimal amount = normalize(request.amount());

        if (amount.compareTo(maxWithdrawalPerTransaction) > 0) {
            throw ApiException.badRequest("WITHDRAWAL_LIMIT_EXCEEDED",
                    "Maximum withdrawal per transaction is " + maxWithdrawalPerTransaction);
        }
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);
        BigDecimal alreadyWithdrawn = transactionRepository.sumByAccountAndTypeAndPeriod(
                account.getId(), TransactionType.WITHDRAWAL, startOfDay, endOfDay);
        BigDecimal dailyLimit = maxDailyWithdrawal;
        if (account.getAccountType().getDailyWithdrawalLimit() != null
                && account.getAccountType().getDailyWithdrawalLimit().compareTo(BigDecimal.ZERO) > 0) {
            dailyLimit = dailyLimit.min(account.getAccountType().getDailyWithdrawalLimit());
        }
        if (alreadyWithdrawn.add(amount).compareTo(dailyLimit) > 0) {
            throw ApiException.badRequest("DAILY_WITHDRAWAL_LIMIT_EXCEEDED",
                    "Daily withdrawal limit is " + dailyLimit + ". Already withdrawn today: " + alreadyWithdrawn);
        }
        BigDecimal withdrawable = account.getBalance().subtract(account.getAccountType().getMinimumBalance());
        if (amount.compareTo(withdrawable) > 0) {
            throw ApiException.badRequest("INSUFFICIENT_BALANCE",
                    "Insufficient available balance. Minimum balance requirement is "
                            + account.getAccountType().getMinimumBalance());
        }

        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        account.debit(amount);
        accountRepository.save(account);

        Transaction transaction = buildTransaction(account, null, TransactionType.WITHDRAWAL,
                amount, account.getBalance(), "Cash withdrawal" + descriptionSuffix(request.description()),
                group, reference);
        transactionRepository.save(transaction);
        writeLedger(group, reference, account.getAccountNumber(), LedgerEntryType.DEBIT, amount, "Withdrawal debited");
        writeLedger(group, reference, "CASH", LedgerEntryType.CREDIT, amount, "Cash paid out");

        notificationService.notify(account.getCustomer().getUser(), "Withdrawal completed",
                format(amount) + " was withdrawn from account " + account.getAccountNumber() + ".",
                NotificationType.TRANSACTION, "/transactions");
        auditService.log("WITHDRAWAL", "Account", account.getId(),
                "Withdrawal " + format(amount) + " reference " + reference);
        return TransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionDtos.TransferResponse transfer(TransactionDtos.TransferRequest request,
                                                      String idempotencyHeader) {
        String key = firstNonBlank(idempotencyHeader, request.idempotencyKey());
        if (!StringUtils.hasText(key)) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED",
                    "X-Idempotency-Key header is required for fund transfers");
        }
        key = key.trim();
        if (key.length() > 100) {
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY", "Idempotency key must be 100 characters or fewer");
        }
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw ApiException.badRequest("SELF_TRANSFER_NOT_ALLOWED", "Source and destination accounts must differ");
        }

        String currentEmail = currentEmail();
        IdempotencyRecord existing = idempotencyRecordRepository.findByIdempotencyKey(key).orElse(null);
        if (existing != null) {
            if (!existing.getUserEmail().equalsIgnoreCase(currentEmail)
                    || !"TRANSFER".equals(existing.getOperation())) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT",
                        "This idempotency key was already used for a different operation");
            }
            if (existing.getStatus() == IdempotencyStatus.COMPLETED && existing.getTransactionReference() != null) {
                return replayTransfer(existing.getTransactionReference());
            }
            throw ApiException.conflict("TRANSFER_IN_PROGRESS",
                    "A transfer with this idempotency key is already being processed");
        }

        BigDecimal amount = normalize(request.amount());
        List<Account> locked = accountRepository.findForUpdate(
                List.of(request.fromAccountNumber(), request.toAccountNumber()));
        Map<String, Account> byNumber = new LinkedHashMap<>();
        locked.forEach(a -> byNumber.put(a.getAccountNumber(), a));
        Account from = byNumber.get(request.fromAccountNumber());
        Account to = byNumber.get(request.toAccountNumber());
        if (from == null || to == null) {
            throw ApiException.notFound("ACCOUNT_NOT_FOUND", "One or both accounts were not found");
        }
        assertAccountAccess(from);
        ensureActive(from);
        ensureActive(to);
        if (from.isDeleted() || to.isDeleted()) {
            throw ApiException.notFound("ACCOUNT_NOT_FOUND", "One or both accounts are closed");
        }
        BigDecimal fromWithdrawable = from.getBalance().subtract(from.getAccountType().getMinimumBalance());
        if (amount.compareTo(fromWithdrawable) > 0) {
            throw ApiException.badRequest("INSUFFICIENT_BALANCE", "Insufficient available balance for this transfer");
        }

        IdempotencyRecord reservation = new IdempotencyRecord();
        reservation.setIdempotencyKey(key);
        reservation.setOperation("TRANSFER");
        reservation.setUserEmail(currentEmail);
        reservation.setStatus(IdempotencyStatus.PROCESSING);
        // Unique constraint is the final guard against duplicate concurrent transfers.
        idempotencyRecordRepository.saveAndFlush(reservation);

        String group = ReferenceGenerator.transactionGroup();
        String debitReference = ReferenceGenerator.transactionReference();
        String creditReference = ReferenceGenerator.transactionReference();
        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        Transaction debit = buildTransaction(from, to.getAccountNumber(), TransactionType.TRANSFER_DEBIT,
                amount, from.getBalance(), "Transfer to " + to.getAccountNumber()
                        + descriptionSuffix(request.description()), group, debitReference);
        Transaction credit = buildTransaction(to, from.getAccountNumber(), TransactionType.TRANSFER_CREDIT,
                amount, to.getBalance(), "Transfer from " + from.getAccountNumber()
                        + descriptionSuffix(request.description()), group, creditReference);
        transactionRepository.save(debit);
        transactionRepository.save(credit);

        writeLedger(group, debitReference, from.getAccountNumber(), LedgerEntryType.DEBIT,
                amount, "Transfer to " + to.getAccountNumber());
        writeLedger(group, creditReference, to.getAccountNumber(), LedgerEntryType.CREDIT,
                amount, "Transfer from " + from.getAccountNumber());

        reservation.setStatus(IdempotencyStatus.COMPLETED);
        reservation.setTransactionReference(debitReference);
        idempotencyRecordRepository.save(reservation);

        notificationService.notify(from.getCustomer().getUser(), "Transfer successful",
                format(amount) + " was transferred from " + from.getAccountNumber()
                        + " to " + to.getAccountNumber() + ". Reference: " + debitReference,
                NotificationType.TRANSACTION, "/transactions");
        notificationService.notify(to.getCustomer().getUser(), "Money received",
                format(amount) + " was received in account " + to.getAccountNumber()
                        + " from " + from.getAccountNumber() + ". Reference: " + creditReference,
                NotificationType.TRANSACTION, "/transactions");
        auditService.log("TRANSFER", "Account", from.getId(),
                "Transferred " + format(amount) + " from " + from.getAccountNumber()
                        + " to " + to.getAccountNumber() + " (group " + group + ")");

        return new TransactionDtos.TransferResponse(
                group, debitReference, from.getAccountNumber(), to.getAccountNumber(), amount,
                from.getBalance(), to.getBalance(),
                TransactionMapper.toResponse(debit), TransactionMapper.toResponse(credit), false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDtos.TransactionResponse> list(Long accountId, Long customerId,
                                                                  TransactionType type, TransactionStatus status,
                                                                  Instant from, Instant to, String query,
                                                                  int page, int size, String sortBy, String sortDir) {
        Long effectiveCustomerId = customerId;
        if (!SecurityUtils.isStaff()) {
            effectiveCustomerId = authenticatedUserService.customer().getId();
        }
        if (accountId != null) {
            Account account = accountRepository.findByIdAndDeletedFalse(accountId)
                    .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
            assertAccountAccess(account);
        }
        Long finalCustomerId = effectiveCustomerId;
        Specification<Transaction> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("id"), accountId));
            }
            if (finalCustomerId != null) {
                predicates.add(cb.equal(root.get("account").get("customer").get("id"), finalCustomerId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (StringUtils.hasText(query)) {
                String like = "%" + query.trim().toLowerCase() + "%";
                Join<Transaction, Account> account = root.join("account");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("reference")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("counterpartyAccountNumber"), "")), like),
                        cb.like(cb.lower(account.get("accountNumber")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PaginationUtil.pageable(page, size, sortBy, sortDir);
        Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);
        return new PageResponse<>(
                transactions.getContent().stream().map(TransactionMapper::toResponse).toList(),
                transactions.getNumber(), transactions.getSize(), transactions.getTotalElements(),
                transactions.getTotalPages(), transactions.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDtos.TransactionResponse get(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND", "Transaction not found"));
        assertAccountAccess(transaction.getAccount());
        return TransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDtos.TransactionResponse> byAccount(Long accountId, int page, int size) {
        Account account = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        assertAccountAccess(account);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);
        return new PageResponse<>(
                transactions.getContent().stream().map(TransactionMapper::toResponse).toList(),
                transactions.getNumber(), transactions.getSize(), transactions.getTotalElements(),
                transactions.getTotalPages(), transactions.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDtos.StatementResponse statement(String accountNumber, Instant from, Instant to) {
        Account account = accountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        assertAccountAccess(account);
        Instant rangeTo = to == null ? Instant.now() : to;
        Instant rangeFrom = from == null ? rangeTo.minus(30, ChronoUnit.DAYS) : from;
        if (rangeFrom.isAfter(rangeTo)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "From date must be before to date");
        }
        if (Duration.between(rangeFrom, rangeTo).toDays() > 366) {
            throw ApiException.badRequest("DATE_RANGE_TOO_LARGE", "Statement range cannot exceed 366 days");
        }

        List<Transaction> afterFrom = transactionRepository
                .findByAccountIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(account.getId(), rangeFrom);
        BigDecimal netAfterFrom = afterFrom.stream()
                .map(t -> CREDIT_TYPES.contains(t.getType()) ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingBalance = account.getBalance().subtract(netAfterFrom);

        List<Transaction> periodTransactions = afterFrom.stream()
                .filter(t -> !t.getCreatedAt().isAfter(rangeTo))
                .toList();
        BigDecimal credits = periodTransactions.stream()
                .filter(t -> CREDIT_TYPES.contains(t.getType()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debits = periodTransactions.stream()
                .filter(t -> !CREDIT_TYPES.contains(t.getType()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closing = openingBalance.add(credits).subtract(debits);

        return new TransactionDtos.StatementResponse(
                account.getAccountNumber(),
                account.getCustomer().getUser().getFullName(),
                "Northstar Bank",
                rangeFrom,
                rangeTo,
                openingBalance,
                closing,
                credits,
                debits,
                periodTransactions.stream().map(TransactionMapper::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] statementPdf(String accountNumber, Instant from, Instant to) {
        return statementPdfGenerator.generate(statement(accountNumber, from, to));
    }

    private Account lockAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        return accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
    }

    private void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("ACCOUNT_NOT_ACTIVE",
                    "Account " + account.getAccountNumber() + " is " + account.getStatus());
        }
    }

    private void assertAccountAccess(Account account) {
        if (SecurityUtils.isStaff()) {
            return;
        }
        var customer = authenticatedUserService.customer();
        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw ApiException.forbidden("ACCOUNT_ACCESS_DENIED", "You can only access your own accounts");
        }
    }

    private Transaction buildTransaction(Account account, String counterparty, TransactionType type,
                                         BigDecimal amount, BigDecimal balanceAfter, String description,
                                         String group, String reference) {
        Transaction transaction = new Transaction();
        transaction.setReference(reference);
        transaction.setTransactionGroup(group);
        transaction.setAccount(account);
        transaction.setCounterpartyAccountNumber(counterparty);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setPerformedBy(currentEmail());
        return transaction;
    }

    private void writeLedger(String group, String reference, String accountNumber,
                             LedgerEntryType type, BigDecimal amount, String description) {
        ledgerEntryRepository.save(new LedgerEntry(group, reference, accountNumber, type, amount, description));
    }

    private TransactionDtos.TransferResponse replayTransfer(String debitReference) {
        Transaction debit = transactionRepository.findByReference(debitReference)
                .orElseThrow(() -> ApiException.conflict("IDEMPOTENCY_REPLAY_UNAVAILABLE",
                        "Completed transfer record is unavailable"));
        List<Transaction> group = transactionRepository
                .findByTransactionGroupOrderByCreatedAtAsc(debit.getTransactionGroup());
        Transaction credit = group.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_CREDIT)
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("IDEMPOTENCY_REPLAY_UNAVAILABLE",
                        "Completed transfer credit record is unavailable"));
        return new TransactionDtos.TransferResponse(
                debit.getTransactionGroup(), debit.getReference(),
                debit.getAccount().getAccountNumber(), credit.getAccount().getAccountNumber(),
                debit.getAmount(), debit.getBalanceAfter(), credit.getBalanceAfter(),
                TransactionMapper.toResponse(debit), TransactionMapper.toResponse(credit), true);
    }

    private BigDecimal normalize(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Amount must be greater than zero");
        }
        return normalized;
    }

    private String currentEmail() {
        return SecurityUtils.currentEmail() == null ? "system" : SecurityUtils.currentEmail();
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String descriptionSuffix(String description) {
        return StringUtils.hasText(description) ? " - " + description.trim() : "";
    }

    private String format(BigDecimal amount) {
        return "INR " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

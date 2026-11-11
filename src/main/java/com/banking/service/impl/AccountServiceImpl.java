package com.banking.service.impl;

import com.banking.dto.AccountDtos;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.entity.Account;
import com.banking.entity.AccountType;
import com.banking.entity.Branch;
import com.banking.entity.Customer;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Transaction;
import com.banking.enums.AccountStatus;
import com.banking.enums.LedgerEntryType;
import com.banking.enums.NotificationType;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exception.ApiException;
import com.banking.mapper.AccountMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.AccountTypeRepository;
import com.banking.repository.BranchRepository;
import com.banking.repository.CustomerRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.TransactionRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AccountService;
import com.banking.service.AuditService;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.NotificationService;
import com.banking.util.PaginationUtil;
import com.banking.util.ReferenceGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public AccountDtos.AccountResponse open(AccountDtos.OpenAccountRequest request) {
        Customer customer = resolveCustomerForOpening(request.customerId());
        AccountType accountType = accountTypeRepository.findByCodeIgnoreCase(request.accountTypeCode())
                .filter(AccountType::isActive)
                .orElseThrow(() -> ApiException.badRequest("ACCOUNT_TYPE_NOT_FOUND", "Invalid or inactive account type"));
        Branch branch = branchRepository.findByCodeIgnoreCase(request.branchCode())
                .filter(Branch::isActive)
                .orElseThrow(() -> ApiException.badRequest("BRANCH_NOT_FOUND", "Invalid or inactive branch"));

        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setCustomer(customer);
        account.setAccountType(accountType);
        account.setBranch(branch);
        account.setNickname(request.nickname());
        account.setInterestRate(accountType.getInterestRate());
        account.setBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("INR");
        accountRepository.save(account);

        BigDecimal initialDeposit = request.initialDeposit() == null ? BigDecimal.ZERO : request.initialDeposit();
        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            String group = ReferenceGenerator.transactionGroup();
            String reference = ReferenceGenerator.transactionReference();
            account.credit(initialDeposit);
            accountRepository.save(account);
            Transaction transaction = new Transaction();
            transaction.setReference(reference);
            transaction.setTransactionGroup(group);
            transaction.setAccount(account);
            transaction.setType(TransactionType.DEPOSIT);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setAmount(initialDeposit);
            transaction.setBalanceAfter(account.getBalance());
            transaction.setDescription("Initial deposit at account opening");
            transaction.setPerformedBy(SecurityUtils.currentEmail() == null ? "system" : SecurityUtils.currentEmail());
            transactionRepository.save(transaction);

            ledgerEntryRepository.save(new LedgerEntry(group, reference, "CASH",
                    LedgerEntryType.DEBIT, initialDeposit, "Cash received for initial deposit"));
            ledgerEntryRepository.save(new LedgerEntry(group, reference, account.getAccountNumber(),
                    LedgerEntryType.CREDIT, initialDeposit, "Initial deposit credited"));
        }

        notificationService.notify(customer.getUser(), "Account opened",
                "Your " + accountType.getName() + " account " + account.getAccountNumber()
                        + " is active. Branch: " + branch.getName() + ".",
                NotificationType.SYSTEM, "/accounts");
        auditService.log("ACCOUNT_OPENED", "Account", account.getId(),
                "Account " + account.getAccountNumber() + " opened for customer " + customer.getCustomerNumber());
        return AccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountDtos.AccountResponse> list(AccountStatus status, String query,
                                                          int page, int size, String sortBy, String sortDir) {
        Long customerId = SecurityUtils.isStaff() ? null : authenticatedUserService.customer().getId();
        Specification<Account> spec = accountSpecification(customerId, status, query);
        Pageable pageable = PaginationUtil.pageable(page, size, sortBy, sortDir);
        Page<Account> accounts = accountRepository.findAll(spec, pageable);
        return new PageResponse<>(
                accounts.getContent().stream().map(AccountMapper::toResponse).toList(),
                accounts.getNumber(), accounts.getSize(), accounts.getTotalElements(),
                accounts.getTotalPages(), accounts.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDtos.AccountResponse get(Long id) {
        Account account = findAccount(id);
        assertAccountAccess(account);
        return AccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountDtos.AccountResponse> byCustomer(Long customerId, int page, int size) {
        if (!SecurityUtils.isStaff() && !authenticatedUserService.customer().getId().equals(customerId)) {
            throw ApiException.forbidden("ACCOUNT_ACCESS_DENIED", "You can only view your own accounts");
        }
        if (!customerRepository.existsById(customerId)) {
            throw ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer not found");
        }
        Pageable pageable = PaginationUtil.pageable(page, size, "openedAt", "desc");
        Page<Account> accounts = accountRepository.findByCustomerIdAndDeletedFalse(customerId, pageable);
        return new PageResponse<>(
                accounts.getContent().stream().map(AccountMapper::toResponse).toList(),
                accounts.getNumber(), accounts.getSize(), accounts.getTotalElements(),
                accounts.getTotalPages(), accounts.isLast());
    }

    @Override
    @Transactional
    public AccountDtos.AccountResponse updateStatus(Long id, AccountDtos.UpdateAccountStatusRequest request) {
        Account account = findAccount(id);
        if (request.status() == account.getStatus()) {
            return AccountMapper.toResponse(account);
        }
        if (request.status() == AccountStatus.CLOSED
                && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw ApiException.conflict("ACCOUNT_BALANCE_NOT_ZERO",
                    "Account balance must be zero before closing the account");
        }
        account.setStatus(request.status());
        accountRepository.save(account);
        notificationService.notify(account.getCustomer().getUser(), "Account status updated",
                "Account " + account.getAccountNumber() + " is now " + request.status() + ".",
                NotificationType.SYSTEM, "/accounts");
        auditService.log("ACCOUNT_STATUS_CHANGED", "Account", account.getId(),
                "Status changed to " + request.status());
        return AccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDtos.AccountBalanceResponse balance(Long id) {
        Account account = findAccount(id);
        assertAccountAccess(account);
        return new AccountDtos.AccountBalanceResponse(
                account.getAccountNumber(), account.getBalance(), account.getAvailableBalance(),
                account.getCurrency(), account.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDtos.AccountTypeResponse> accountTypes() {
        return accountTypeRepository.findByActiveTrueOrderByName().stream()
                .map(AccountMapper::toTypeResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDtos.BranchOption> branches() {
        return branchRepository.findByActiveTrueOrderByName().stream()
                .map(AccountMapper::toBranchOption).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Account requireAccessibleAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        assertAccountAccess(account);
        return account;
    }

    @Override
    public AccountDtos.AccountResponse toResponse(Account account) {
        return AccountMapper.toResponse(account);
    }

    private Customer resolveCustomerForOpening(Long requestedCustomerId) {
        if (SecurityUtils.isStaff()) {
            if (requestedCustomerId == null) {
                throw ApiException.badRequest("CUSTOMER_REQUIRED", "customerId is required when opening an account as staff");
            }
            return customerRepository.findById(requestedCustomerId)
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer not found"));
        }
        Customer customer = authenticatedUserService.customer();
        if (requestedCustomerId != null && !requestedCustomerId.equals(customer.getId())) {
            throw ApiException.forbidden("CUSTOMER_ACCESS_DENIED", "You can only open an account for yourself");
        }
        return customer;
    }

    private Specification<Account> accountSpecification(Long customerId, AccountStatus status, String query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(query)) {
                String like = "%" + query.trim().toLowerCase() + "%";
                Join<Account, Customer> customer = root.join("customer");
                Join<Customer, com.banking.entity.User> user = customer.join("user");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accountNumber")), like),
                        cb.like(cb.lower(customer.get("customerNumber")), like),
                        cb.like(cb.lower(user.get("firstName")), like),
                        cb.like(cb.lower(user.get("lastName")), like),
                        cb.like(cb.lower(user.get("email")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("nickname"), "")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Account findAccount(Long id) {
        return accountRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
    }

    private void assertAccountAccess(Account account) {
        if (SecurityUtils.isStaff()) {
            return;
        }
        Customer current = authenticatedUserService.customer();
        if (!account.getCustomer().getId().equals(current.getId())) {
            throw ApiException.forbidden("ACCOUNT_ACCESS_DENIED", "You can only access your own accounts");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < 30; attempt++) {
            String number = ReferenceGenerator.accountNumber();
            if (!accountRepository.existsByAccountNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }
}

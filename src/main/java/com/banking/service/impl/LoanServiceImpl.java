package com.banking.service.impl;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.LoanDtos;
import com.banking.entity.Account;
import com.banking.entity.Customer;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Loan;
import com.banking.entity.LoanPayment;
import com.banking.entity.Transaction;
import com.banking.enums.AccountStatus;
import com.banking.enums.LedgerEntryType;
import com.banking.enums.LoanPaymentStatus;
import com.banking.enums.LoanStatus;
import com.banking.enums.NotificationType;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exception.ApiException;
import com.banking.mapper.LoanMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.LoanPaymentRepository;
import com.banking.repository.LoanRepository;
import com.banking.repository.TransactionRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AuditService;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.LoanService;
import com.banking.service.NotificationService;
import com.banking.util.EmiCalculator;
import com.banking.util.PageMapper;
import com.banking.util.PaginationUtil;
import com.banking.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private static final Map<String, BigDecimal> INTEREST_RATES = Map.of(
            "PERSONAL", new BigDecimal("13.500"),
            "HOME", new BigDecimal("8.500"),
            "AUTO", new BigDecimal("10.500"),
            "EDUCATION", new BigDecimal("9.500"),
            "BUSINESS", new BigDecimal("14.500"));

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AuthenticatedUserService authenticatedUserService;

    @Value("${banking.loan.max-principal:5000000}")
    private BigDecimal maxPrincipal;

    @Override
    @Transactional
    public LoanDtos.LoanResponse apply(LoanDtos.LoanApplicationRequest request) {
        Customer customer = authenticatedUserService.customer();
        Account account = accountRepository.findByAccountNumberAndDeletedFalse(request.disbursementAccountNumber())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Disbursement account not found"));
        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw ApiException.forbidden("ACCOUNT_ACCESS_DENIED", "Disbursement account must belong to you");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("ACCOUNT_NOT_ACTIVE", "Disbursement account is not active");
        }
        if (request.principal().compareTo(maxPrincipal) > 0) {
            throw ApiException.badRequest("LOAN_LIMIT_EXCEEDED", "Maximum loan amount is " + maxPrincipal);
        }
        String loanType = normalizeType(request.loanType());
        BigDecimal rate = INTEREST_RATES.get(loanType);
        if (rate == null) {
            throw ApiException.badRequest("UNSUPPORTED_LOAN_TYPE",
                    "Supported loan types: PERSONAL, HOME, AUTO, EDUCATION, BUSINESS");
        }
        BigDecimal emi = EmiCalculator.monthlyEmi(request.principal(), rate, request.termMonths());
        BigDecimal totalPayable = EmiCalculator.totalPayable(emi, request.termMonths());

        Loan loan = new Loan();
        loan.setLoanNumber(generateUniqueLoanNumber());
        loan.setCustomer(customer);
        loan.setDisbursementAccount(account);
        loan.setLoanType(loanType);
        loan.setPrincipal(request.principal().setScale(2, RoundingMode.HALF_UP));
        loan.setAnnualInterestRate(rate);
        loan.setTermMonths(request.termMonths());
        loan.setEmi(emi);
        loan.setTotalPayable(totalPayable);
        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.PENDING);
        loan.setPurpose(request.purpose());
        loan.setAppliedAt(Instant.now());
        loanRepository.save(loan);

        notificationService.notify(customer.getUser(), "Loan application submitted",
                "Your " + loanType + " loan application " + loan.getLoanNumber()
                        + " is pending review. EMI: INR " + emi + " for " + request.termMonths() + " months.",
                NotificationType.LOAN, "/loans");
        auditService.log("LOAN_APPLIED", "Loan", loan.getId(),
                "Loan " + loan.getLoanNumber() + " applied for " + loan.getPrincipal());
        return LoanMapper.toResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanDtos.LoanResponse> list(LoanStatus status, int page, int size,
                                                    String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.pageable(page, size, sortBy, sortDir);
        Page<Loan> loans;
        if (SecurityUtils.isStaff()) {
            loans = status == null
                    ? loanRepository.findAll(pageable)
                    : loanRepository.findByStatus(status, pageable);
        } else {
            Customer customer = authenticatedUserService.customer();
            loans = loanRepository.findByCustomerId(customer.getId(), pageable);
            if (status != null) {
                List<LoanDtos.LoanResponse> filtered = loans.getContent().stream()
                        .filter(l -> l.getStatus() == status)
                        .map(LoanMapper::toResponse).toList();
                return PageMapper.of(filtered, loans.getNumber(), loans.getSize(), filtered.size());
            }
        }
        return new PageResponse<>(
                loans.getContent().stream().map(LoanMapper::toResponse).toList(),
                loans.getNumber(), loans.getSize(), loans.getTotalElements(),
                loans.getTotalPages(), loans.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public LoanDtos.LoanResponse get(Long id) {
        return LoanMapper.toResponse(loadAccessibleLoan(id));
    }

    @Override
    @Transactional
    public LoanDtos.LoanResponse approve(Long id, LoanDtos.LoanDecisionRequest request) {
        Loan loan = lockLoan(id);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw ApiException.conflict("LOAN_NOT_PENDING", "Only pending loans can be approved");
        }
        Account account = loan.getDisbursementAccount();
        if (account == null) {
            throw ApiException.badRequest("DISBURSEMENT_ACCOUNT_MISSING", "Loan has no disbursement account");
        }
        account = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Disbursement account not found"));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("ACCOUNT_NOT_ACTIVE", "Disbursement account is not active");
        }

        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        BigDecimal principal = loan.getPrincipal();
        account.credit(principal);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setReference(reference);
        transaction.setTransactionGroup(group);
        transaction.setAccount(account);
        transaction.setType(TransactionType.LOAN_DISBURSEMENT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmount(principal);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription("Loan disbursement for " + loan.getLoanNumber());
        transaction.setPerformedBy(currentEmail());
        transactionRepository.save(transaction);
        ledgerEntryRepository.save(new LedgerEntry(group, reference, "LOAN_RECEIVABLE",
                LedgerEntryType.DEBIT, principal, "Loan principal disbursed"));
        ledgerEntryRepository.save(new LedgerEntry(group, reference, account.getAccountNumber(),
                LedgerEntryType.CREDIT, principal, "Loan disbursement credited"));

        List<LoanPayment> schedule = buildSchedule(loan);
        loanPaymentRepository.saveAll(schedule);
        BigDecimal actualTotal = schedule.stream().map(LoanPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        loan.setTotalPayable(actualTotal);
        loan.setOutstandingBalance(actualTotal);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovedAt(Instant.now());
        loan.setApprovedBy(currentEmail());
        loan.setNextDueDate(schedule.get(0).getDueDate());
        loan.setPaidInstallments(0);
        loanRepository.save(loan);

        notificationService.notify(loan.getCustomer().getUser(), "Loan approved and disbursed",
                "Your loan " + loan.getLoanNumber() + " was approved. INR " + principal
                        + " was credited to account " + account.getAccountNumber()
                        + ". First EMI is due on " + loan.getNextDueDate() + ".",
                NotificationType.LOAN, "/loans");
        auditService.log("LOAN_APPROVED", "Loan", loan.getId(),
                "Loan " + loan.getLoanNumber() + " approved and disbursed. " +
                        (request.remarks() == null ? "" : "Remarks: " + request.remarks()));
        return LoanMapper.toResponse(loan);
    }

    @Override
    @Transactional
    public LoanDtos.LoanResponse reject(Long id, LoanDtos.LoanRejectionRequest request) {
        Loan loan = lockLoan(id);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw ApiException.conflict("LOAN_NOT_PENDING", "Only pending loans can be rejected");
        }
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(request.reason());
        loan.setApprovedAt(Instant.now());
        loan.setApprovedBy(currentEmail());
        loanRepository.save(loan);
        notificationService.notify(loan.getCustomer().getUser(), "Loan application rejected",
                "Your loan " + loan.getLoanNumber() + " was rejected. Reason: " + request.reason(),
                NotificationType.LOAN, "/loans");
        auditService.log("LOAN_REJECTED", "Loan", loan.getId(),
                "Loan " + loan.getLoanNumber() + " rejected. Reason: " + request.reason());
        return LoanMapper.toResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanDtos.LoanScheduleResponse schedule(Long id) {
        Loan loan = loadAccessibleLoan(id);
        List<LoanPayment> payments = loanPaymentRepository.findByLoanIdOrderByDueDateAsc(loan.getId());
        return new LoanDtos.LoanScheduleResponse(
                loan.getLoanNumber(), loan.getEmi(), loan.getTermMonths(), loan.getPaidInstallments(),
                loan.getOutstandingBalance(), LoanMapper.toSchedule(payments));
    }

    @Override
    @Transactional
    public LoanDtos.LoanPaymentResponse pay(Long id, LoanDtos.LoanPaymentRequest request) {
        Loan loan = lockLoan(id);
        assertLoanAccess(loan);
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw ApiException.badRequest("LOAN_NOT_ACTIVE", "Only active loans can be repaid");
        }
        Account account = accountRepository.findByAccountNumberAndDeletedFalse(request.accountNumber())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Repayment account not found"));
        if (!account.getCustomer().getId().equals(loan.getCustomer().getId())) {
            throw ApiException.forbidden("ACCOUNT_ACCESS_DENIED",
                    "Repayment must come from an account owned by the loan customer");
        }
        account = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Repayment account not found"));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("ACCOUNT_NOT_ACTIVE", "Repayment account is not active");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Repayment amount must be positive");
        }
        if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
            throw ApiException.badRequest("AMOUNT_EXCEEDS_OUTSTANDING",
                    "Repayment amount exceeds outstanding balance of " + loan.getOutstandingBalance());
        }
        BigDecimal withdrawable = account.getBalance().subtract(account.getAccountType().getMinimumBalance());
        if (amount.compareTo(withdrawable) > 0) {
            throw ApiException.badRequest("INSUFFICIENT_BALANCE", "Insufficient balance for this repayment");
        }

        List<LoanPayment> pending = loanPaymentRepository.findByLoanIdOrderByDueDateAsc(loan.getId())
                .stream().filter(p -> p.getStatus() == LoanPaymentStatus.PENDING)
                .sorted(Comparator.comparing(LoanPayment::getDueDate)).toList();
        if (pending.isEmpty()) {
            throw ApiException.conflict("NO_PENDING_INSTALLMENTS", "No pending installments remain");
        }

        boolean fullPayoff = amount.compareTo(loan.getOutstandingBalance()) == 0;
        List<LoanPayment> toPay = new ArrayList<>();
        if (fullPayoff) {
            toPay.addAll(pending);
        } else {
            BigDecimal selected = BigDecimal.ZERO;
            for (LoanPayment payment : pending) {
                if (selected.add(payment.getAmount()).compareTo(amount) > 0) {
                    break;
                }
                selected = selected.add(payment.getAmount());
                toPay.add(payment);
            }
            if (toPay.isEmpty() || selected.compareTo(amount) != 0) {
                throw ApiException.badRequest("PAYMENT_MUST_MATCH_INSTALLMENTS",
                        "Payment must match one or more complete EMIs or the full outstanding balance");
            }
        }

        BigDecimal principalPaid = toPay.stream().map(LoanPayment::getPrincipalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interestPaid = toPay.stream().map(LoanPayment::getInterestComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Use the actual amount for the final adjusted installment.
        BigDecimal ledgerPrincipal = principalPaid;
        BigDecimal ledgerInterest = amount.subtract(principalPaid);
        if (ledgerInterest.compareTo(BigDecimal.ZERO) < 0) {
            ledgerInterest = BigDecimal.ZERO;
            ledgerPrincipal = amount;
        }

        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        account.debit(amount);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setReference(reference);
        transaction.setTransactionGroup(group);
        transaction.setAccount(account);
        transaction.setType(TransactionType.LOAN_REPAYMENT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription("Loan repayment for " + loan.getLoanNumber());
        transaction.setPerformedBy(currentEmail());
        transactionRepository.save(transaction);
        ledgerEntryRepository.save(new LedgerEntry(group, reference, account.getAccountNumber(),
                LedgerEntryType.DEBIT, amount, "Loan repayment debited"));
        ledgerEntryRepository.save(new LedgerEntry(group, reference, "LOAN_RECEIVABLE",
                LedgerEntryType.CREDIT, ledgerPrincipal, "Loan principal repaid"));
        if (ledgerInterest.compareTo(BigDecimal.ZERO) > 0) {
            ledgerEntryRepository.save(new LedgerEntry(group, reference, "INTEREST_INCOME",
                    LedgerEntryType.CREDIT, ledgerInterest, "Loan interest income"));
        }

        Instant now = Instant.now();
        for (LoanPayment payment : toPay) {
            payment.setStatus(LoanPaymentStatus.PAID);
            payment.setPaidAt(now);
            payment.setTransactionReference(reference);
        }
        loanPaymentRepository.saveAll(toPay);

        loan.setPaidInstallments(loan.getPaidInstallments() + toPay.size());
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(amount).max(BigDecimal.ZERO));
        if (loan.getOutstandingBalance().compareTo(new BigDecimal("0.01")) <= 0) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.CLOSED);
            loan.setNextDueDate(null);
        } else {
            loan.setNextDueDate(pending.stream()
                    .filter(p -> !toPay.contains(p))
                    .map(LoanPayment::getDueDate)
                    .findFirst().orElse(null));
        }
        loanRepository.save(loan);

        notificationService.notify(loan.getCustomer().getUser(), "Loan repayment successful",
                "INR " + amount + " was repaid for loan " + loan.getLoanNumber()
                        + ". Outstanding balance: INR " + loan.getOutstandingBalance() + ".",
                NotificationType.LOAN, "/loans");
        auditService.log("LOAN_REPAYMENT", "Loan", loan.getId(),
                "Loan " + loan.getLoanNumber() + " repayment " + amount + " reference " + reference);
        return new LoanDtos.LoanPaymentResponse(
                loan.getLoanNumber(), reference, amount, loan.getOutstandingBalance(),
                loan.getPaidInstallments(), LoanPaymentStatus.PAID, now);
    }

    @Override
    public LoanDtos.EmiCalculationResponse calculateEmi(LoanDtos.EmiCalculationRequest request) {
        BigDecimal emi = EmiCalculator.monthlyEmi(request.principal(), request.annualInterestRate(),
                request.termMonths());
        BigDecimal total = EmiCalculator.totalPayable(emi, request.termMonths());
        return new LoanDtos.EmiCalculationResponse(
                emi, total, EmiCalculator.totalInterest(total, request.principal()));
    }

    private List<LoanPayment> buildSchedule(Loan loan) {
        List<LoanPayment> schedule = new ArrayList<>();
        BigDecimal remaining = loan.getPrincipal();
        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 12, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= loan.getTermMonths(); i++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComponent = i == loan.getTermMonths()
                    ? remaining
                    : loan.getEmi().subtract(interest).max(BigDecimal.ZERO);
            if (principalComponent.compareTo(remaining) > 0) {
                principalComponent = remaining;
            }
            BigDecimal installment = principalComponent.add(interest);
            LoanPayment payment = new LoanPayment();
            payment.setLoan(loan);
            payment.setDueDate(today.plusMonths(i));
            payment.setAmount(installment);
            payment.setPrincipalComponent(principalComponent);
            payment.setInterestComponent(interest);
            payment.setStatus(LoanPaymentStatus.PENDING);
            schedule.add(payment);
            remaining = remaining.subtract(principalComponent);
        }
        return schedule;
    }

    private Loan lockLoan(Long id) {
        Loan loan = loanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("LOAN_NOT_FOUND", "Loan not found"));
        assertLoanAccess(loan);
        return loan;
    }

    private Loan loadAccessibleLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("LOAN_NOT_FOUND", "Loan not found"));
        assertLoanAccess(loan);
        return loan;
    }

    private void assertLoanAccess(Loan loan) {
        if (SecurityUtils.isStaff()) {
            return;
        }
        Customer customer = authenticatedUserService.customer();
        if (!loan.getCustomer().getId().equals(customer.getId())) {
            throw ApiException.forbidden("LOAN_ACCESS_DENIED", "You can only access your own loans");
        }
    }

    private String normalizeType(String loanType) {
        return loanType.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String generateUniqueLoanNumber() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String number = ReferenceGenerator.loanNumber();
            if (loanRepository.findByLoanNumber(number).isEmpty()) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to generate a unique loan number");
    }

    private String currentEmail() {
        return SecurityUtils.currentEmail() == null ? "system" : SecurityUtils.currentEmail();
    }
}

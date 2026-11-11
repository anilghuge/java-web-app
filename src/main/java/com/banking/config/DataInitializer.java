package com.banking.config;

import com.banking.entity.Account;
import com.banking.entity.AccountType;
import com.banking.entity.AuditLog;
import com.banking.entity.Beneficiary;
import com.banking.entity.Branch;
import com.banking.entity.Card;
import com.banking.entity.Customer;
import com.banking.entity.Employee;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Loan;
import com.banking.entity.LoanPayment;
import com.banking.entity.Notification;
import com.banking.entity.Role;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.CardStatus;
import com.banking.enums.CardType;
import com.banking.enums.Gender;
import com.banking.enums.KycStatus;
import com.banking.enums.LedgerEntryType;
import com.banking.enums.LoanPaymentStatus;
import com.banking.enums.LoanStatus;
import com.banking.enums.NotificationType;
import com.banking.enums.RoleName;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.repository.AccountRepository;
import com.banking.repository.AccountTypeRepository;
import com.banking.repository.AuditLogRepository;
import com.banking.repository.BeneficiaryRepository;
import com.banking.repository.BranchRepository;
import com.banking.repository.CardRepository;
import com.banking.repository.CustomerRepository;
import com.banking.repository.EmployeeRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.LoanPaymentRepository;
import com.banking.repository.LoanRepository;
import com.banking.repository.NotificationRepository;
import com.banking.repository.RoleRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.util.EmiCalculator;
import com.banking.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Loads realistic demonstration data. Disable with SEED_ENABLED=false.
 * Demo accounts use simulated money only.
 */
@Component
@Profile("!test")
@Order(2)
@ConditionalOnProperty(name = "banking.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String[] CUSTOMER_NAMES = {
            "Alice Sharma", "Rohan Verma", "Priya Nair", "Vikram Singh", "Ananya Iyer",
            "Arjun Patel", "Sneha Reddy", "Kabir Khan", "Meera Joshi", "Aditya Rao",
            "Isha Gupta", "Rahul Desai", "Nisha Menon", "Sameer Kulkarni", "Pooja Bhat",
            "Karan Malhotra", "Divya Pillai", "Aakash Jain", "Riya Sen", "Manish Chauhan"
    };
    private static final String[] CITIES = {
            "Mumbai", "Pune", "Bengaluru", "Delhi", "Hyderabad", "Chennai",
            "Kolkata", "Ahmedabad", "Jaipur", "Surat"
    };
    private static final String[] STATES = {
            "Maharashtra", "Maharashtra", "Karnataka", "Delhi", "Telangana", "Tamil Nadu",
            "West Bengal", "Gujarat", "Rajasthan", "Gujarat"
    };
    private static final String[] OCCUPATIONS = {
            "Software Engineer", "Product Manager", "Doctor", "Business Owner", "Teacher",
            "Chartered Accountant", "Architect", "Consultant", "Designer", "Analyst"
    };
    private static final String[] BANKS = {
            "HDFC Bank", "ICICI Bank", "State Bank of India", "Axis Bank", "Kotak Mahindra Bank"
    };

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final CardRepository cardRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${banking.seed.enabled:true}")
    private boolean seedEnabled;

    private final Random random = new Random(20261109L);

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (customerRepository.countByDeletedFalse() > 0) {
            log.info("Demo data already present; skipping banking demo seed");
            return;
        }
        log.info("Seeding banking demo data (simulated money only)...");
        List<Branch> branches = seedBranches();
        Role adminRole = role(RoleName.ADMIN);
        Role managerRole = role(RoleName.MANAGER);
        Role employeeRole = role(RoleName.BANK_EMPLOYEE);
        Role customerRole = role(RoleName.CUSTOMER);

        User admin = createUser("admin@bank.com", "Aarav", "Mehta", "Admin@123",
                "+91 98200 10001", adminRole, Instant.now().minus(400, ChronoUnit.DAYS));
        createUser("manager@bank.com", "Neelam", "Kapoor", "Manager@123",
                "+91 98200 10002", managerRole, Instant.now().minus(350, ChronoUnit.DAYS));
        createUser("employee@bank.com", "Rakesh", "Sharma", "Employee@123",
                "+91 98200 10003", employeeRole, Instant.now().minus(300, ChronoUnit.DAYS));
        createUser("priyanka.ops@bank.com", "Priyanka", "Deshmukh", "Staff@123",
                "+91 98200 10004", employeeRole, Instant.now().minus(280, ChronoUnit.DAYS));
        createUser("amit.loans@bank.com", "Amit", "Bose", "Staff@123",
                "+91 98200 10005", employeeRole, Instant.now().minus(260, ChronoUnit.DAYS));
        createUser("neha.support@bank.com", "Neha", "Kulkarni", "Staff@123",
                "+91 98200 10006", employeeRole, Instant.now().minus(240, ChronoUnit.DAYS));

        seedEmployees(branches, managerRole, employeeRole);

        List<Customer> customers = new ArrayList<>();
        List<Account> accounts = new ArrayList<>();
        AccountType savings = accountType("SAVINGS");
        AccountType current = accountType("CURRENT");
        AccountType salary = accountType("SALARY");

        for (int i = 0; i < CUSTOMER_NAMES.length; i++) {
            String[] nameParts = CUSTOMER_NAMES[i].split(" ", 2);
            String email = nameParts[0].toLowerCase(Locale.ROOT) + "@example.com";
            Instant createdAt = Instant.now().minus(20 + random.nextInt(340), ChronoUnit.DAYS);
            User user = createUser(email, nameParts[0], nameParts[1], "Customer@123",
                    "+91 98" + (20000000 + random.nextInt(79999999)), customerRole, createdAt);
            Customer customer = new Customer();
            customer.setCustomerNumber(String.format("CUS%09d", i + 1));
            customer.setUser(user);
            customer.setDateOfBirth(LocalDate.of(1975 + random.nextInt(25), 1 + random.nextInt(12), 1 + random.nextInt(27)));
            customer.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
            customer.setAddress((10 + i) + ", " + (random.nextInt(2) == 0 ? "MG Road" : "Park Street"));
            customer.setCity(CITIES[i % CITIES.length]);
            customer.setState(STATES[i % STATES.length]);
            customer.setPostalCode(String.valueOf(400000 + i * 37));
            customer.setCountry("India");
            customer.setNationalId("AADHAAR" + (100000000000L + i * 7919L));
            customer.setOccupation(OCCUPATIONS[i % OCCUPATIONS.length]);
            customer.setAnnualIncome(BigDecimal.valueOf(600000L + random.nextInt(2400000)));
            customer.setKycStatus(i >= 18 ? KycStatus.PENDING : KycStatus.VERIFIED);
            customer.setKycVerifiedAt(i >= 18 ? null : createdAt.plus(2, ChronoUnit.DAYS));
            customer.setCreatedAt(createdAt);
            customer.setUpdatedAt(createdAt);
            customerRepository.save(customer);
            customers.add(customer);
        }

        int accountSequence = 1;
        for (int i = 0; i < customers.size(); i++) {
            int numberOfAccounts = i < 5 ? 2 : 1;
            for (int j = 0; j < numberOfAccounts; j++) {
                AccountType type = j == 0 ? savings : (i % 6 == 0 ? current : salary);
                Account account = new Account();
                account.setAccountNumber(String.format("10%010d", accountSequence++));
                account.setCustomer(customers.get(i));
                account.setAccountType(type);
                account.setBranch(branches.get(i % branches.size()));
                account.setStatus(AccountStatus.ACTIVE);
                account.setCurrency("INR");
                account.setNickname(j == 0 ? "Primary " + type.getName() : "Everyday " + type.getName());
                account.setInterestRate(type.getInterestRate());
                Instant openedAt = customers.get(i).getCreatedAt().plus(1 + random.nextInt(20), ChronoUnit.DAYS);
                account.setOpenedAt(openedAt.isAfter(Instant.now()) ? Instant.now().minus(1, ChronoUnit.DAYS) : openedAt);
                account.setDeleted(false);
                accountRepository.save(account);

                BigDecimal opening = money(35000 + random.nextInt(465000));
                depositSeed(account, opening, account.getOpenedAt().plus(2, ChronoUnit.HOURS),
                        "Initial deposit", "branch.operations@bank.com");
                accounts.add(account);
            }
        }

        seedActivity(accounts, customers);
        seedBeneficiaries(customers, accounts);
        seedLoans(customers, accounts);
        seedCards(customers, accounts);
        seedNotifications(customers, admin);
        seedAuditLogs();

        log.info("Demo data ready: {} customers, {} accounts, {} transactions, {} loans, {} cards",
                customerRepository.countByDeletedFalse(), accountRepository.countByDeletedFalse(),
                transactionRepository.count(), loanRepository.count(), cardRepository.count());
        log.info("Demo logins -> admin@bank.com/Admin@123, manager@bank.com/Manager@123, " +
                "employee@bank.com/Employee@123, alice@example.com/Customer@123");
    }

    private List<Branch> seedBranches() {
        if (branchRepository.count() == 0) {
            branch("BR001", "Mumbai Central", "Bandra Kurla Complex, Plot C-21", "Mumbai",
                    "Maharashtra", "400051", "+91 22 4000 1001", "mumbai.central@bank.com", "NSBK0000001");
            branch("BR002", "Pune Camp", "12, East Street, Camp", "Pune",
                    "Maharashtra", "411001", "+91 20 4000 2002", "pune.camp@bank.com", "NSBK0000002");
            branch("BR003", "Bengaluru Tech Park", "Tower B, Whitefield Main Road", "Bengaluru",
                    "Karnataka", "560066", "+91 80 4000 3003", "bengaluru.techpark@bank.com", "NSBK0000003");
        }
        return branchRepository.findByActiveTrueOrderByName();
    }

    private void branch(String code, String name, String address, String city, String state,
                        String postalCode, String phone, String email, String ifsc) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(name);
        branch.setAddress(address);
        branch.setCity(city);
        branch.setState(state);
        branch.setPostalCode(postalCode);
        branch.setPhone(phone);
        branch.setEmail(email);
        branch.setIfscCode(ifsc);
        branch.setActive(true);
        branchRepository.save(branch);
    }

    private void seedEmployees(List<Branch> branches, Role managerRole, Role employeeRole) {
        User manager = userRepository.findByEmailIgnoreCase("manager@bank.com").orElseThrow();
        User employee = userRepository.findByEmailIgnoreCase("employee@bank.com").orElseThrow();
        User ops = userRepository.findByEmailIgnoreCase("priyanka.ops@bank.com").orElseThrow();
        User loans = userRepository.findByEmailIgnoreCase("amit.loans@bank.com").orElseThrow();
        User support = userRepository.findByEmailIgnoreCase("neha.support@bank.com").orElseThrow();
        if (employeeRepository.findByUserId(manager.getId()).isEmpty()) {
            employee("EMP000001", manager, branches.get(0), "Branch Manager",
                    LocalDate.now().minusYears(6), new BigDecimal("145000.00"));
            employee("EMP000002", employee, branches.get(0), "Banking Operations Officer",
                    LocalDate.now().minusYears(4), new BigDecimal("78000.00"));
            employee("EMP000003", ops, branches.get(1), "Operations Officer",
                    LocalDate.now().minusYears(3), new BigDecimal("72000.00"));
            employee("EMP000004", loans, branches.get(2), "Loan Officer",
                    LocalDate.now().minusYears(2), new BigDecimal("85000.00"));
            employee("EMP000005", support, branches.get(2), "Customer Support Executive",
                    LocalDate.now().minusYears(1), new BigDecimal("56000.00"));
        }
    }

    private void employee(String code, User user, Branch branch, String designation,
                          LocalDate joiningDate, BigDecimal salary) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setUser(user);
        employee.setBranch(branch);
        employee.setDesignation(designation);
        employee.setJoiningDate(joiningDate);
        employee.setSalary(salary);
        employee.setActive(true);
        employeeRepository.save(employee);
    }

    private void seedActivity(List<Account> accounts, List<Customer> customers) {
        Instant cursor = Instant.now().minus(55, ChronoUnit.DAYS);
        for (int i = 0; i < 10; i++) {
            Account account = accounts.get(random.nextInt(accounts.size()));
            BigDecimal amount = money(1500 + random.nextInt(18000));
            if (account.getBalance().subtract(amount)
                    .compareTo(account.getAccountType().getMinimumBalance()) > 0) {
                withdrawalSeed(account, amount, cursor, "ATM withdrawal", "customer@bank.com");
            }
            cursor = cursor.plus(1 + random.nextInt(3), ChronoUnit.DAYS)
                    .plus(random.nextInt(10), ChronoUnit.HOURS);
        }
        for (int i = 0; i < 8; i++) {
            Account account = accounts.get(random.nextInt(accounts.size()));
            depositSeed(account, money(5000 + random.nextInt(45000)), cursor,
                    "Salary / inward remittance", "branch.operations@bank.com");
            cursor = cursor.plus(1 + random.nextInt(3), ChronoUnit.DAYS)
                    .plus(random.nextInt(10), ChronoUnit.HOURS);
        }
        for (int i = 0; i < 8; i++) {
            Account from = accounts.get(random.nextInt(accounts.size()));
            Account to = accounts.get(random.nextInt(accounts.size()));
            if (from.getId().equals(to.getId())) {
                continue;
            }
            BigDecimal amount = money(1000 + random.nextInt(24000));
            if (from.getBalance().subtract(amount)
                    .compareTo(from.getAccountType().getMinimumBalance()) > 0) {
                transferSeed(from, to, amount, cursor);
            }
            cursor = cursor.plus(1 + random.nextInt(4), ChronoUnit.DAYS)
                    .plus(random.nextInt(10), ChronoUnit.HOURS);
        }
    }

    private void seedBeneficiaries(List<Customer> customers, List<Account> accounts) {
        for (int i = 0; i < 10; i++) {
            Beneficiary beneficiary = new Beneficiary();
            beneficiary.setCustomer(customers.get(i));
            beneficiary.setName("Beneficiary " + (i + 1));
            beneficiary.setAccountNumber(accounts.get((i + 10) % accounts.size()).getAccountNumber());
            beneficiary.setBankName(BANKS[i % BANKS.length]);
            beneficiary.setBranchName(CITIES[(i + 3) % CITIES.length] + " Main Branch");
            beneficiary.setIfscCode("HDFC0001" + (200 + i));
            beneficiary.setNickname("Payee " + (i + 1));
            beneficiary.setDailyLimit(money(150000 + random.nextInt(150000)));
            beneficiary.setActive(true);
            beneficiary.setCreatedAt(Instant.now().minus(10 + i, ChronoUnit.DAYS));
            beneficiary.setUpdatedAt(beneficiary.getCreatedAt());
            beneficiaryRepository.save(beneficiary);
        }
    }

    private void seedLoans(List<Customer> customers, List<Account> accounts) {
        Instant now = Instant.now();
        seedLoan(customers.get(0), accounts.get(0), "LN2026000001", "PERSONAL",
                new BigDecimal("500000.00"), new BigDecimal("13.500"), 24,
                LoanStatus.ACTIVE, "Home renovation", now.minus(32, ChronoUnit.DAYS), true);
        seedLoan(customers.get(1), accounts.get(customerAccountIndex(1)), "LN2026000002", "HOME",
                new BigDecimal("2500000.00"), new BigDecimal("8.500"), 120,
                LoanStatus.ACTIVE, "Apartment down payment", now.minus(75, ChronoUnit.DAYS), true);
        seedLoan(customers.get(2), accounts.get(customerAccountIndex(2)), "LN2026000003", "PERSONAL",
                new BigDecimal("750000.00"), new BigDecimal("13.500"), 36,
                LoanStatus.PENDING, "Medical expenses", now.minus(4, ChronoUnit.DAYS), false);
        seedLoan(customers.get(3), accounts.get(customerAccountIndex(3)), "LN2026000004", "AUTO",
                new BigDecimal("800000.00"), new BigDecimal("10.500"), 60,
                LoanStatus.PENDING, "Electric SUV purchase", now.minus(2, ChronoUnit.DAYS), false);
        Loan rejected = seedLoan(customers.get(4), accounts.get(customerAccountIndex(4)), "LN2026000005",
                "BUSINESS", new BigDecimal("1200000.00"), new BigDecimal("14.500"), 36,
                LoanStatus.REJECTED, "Working capital", now.minus(20, ChronoUnit.DAYS), false);
        rejected.setRejectionReason("Insufficient business vintage and collateral documentation");
        rejected.setApprovedAt(now.minus(18, ChronoUnit.DAYS));
        rejected.setApprovedBy("manager@bank.com");
        loanRepository.save(rejected);
    }

    private int customerAccountIndex(int customerIndex) {
        return customerIndex < 5 ? customerIndex * 2 : 10 + (customerIndex - 5);
    }

    private Loan seedLoan(Customer customer, Account account, String number, String type,
                          BigDecimal principal, BigDecimal rate, int months, LoanStatus status,
                          String purpose, Instant appliedAt, boolean disburse) {
        BigDecimal emi = EmiCalculator.monthlyEmi(principal, rate, months);
        BigDecimal total = EmiCalculator.totalPayable(emi, months);
        Loan loan = new Loan();
        loan.setLoanNumber(number);
        loan.setCustomer(customer);
        loan.setDisbursementAccount(account);
        loan.setLoanType(type);
        loan.setPrincipal(principal);
        loan.setAnnualInterestRate(rate);
        loan.setTermMonths(months);
        loan.setEmi(emi);
        loan.setTotalPayable(total);
        loan.setPurpose(purpose);
        loan.setAppliedAt(appliedAt);
        loan.setStatus(status);
        if (status == LoanStatus.ACTIVE) {
            loan.setOutstandingBalance(total);
            loan.setApprovedAt(appliedAt.plus(2, ChronoUnit.DAYS));
            loan.setApprovedBy("manager@bank.com");
            loan.setNextDueDate(LocalDate.ofInstant(loan.getApprovedAt(), ZoneOffset.UTC).plusMonths(1));
        } else if (status == LoanStatus.REJECTED) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
        } else {
            loan.setOutstandingBalance(BigDecimal.ZERO);
        }
        loan.setCreatedAt(appliedAt);
        loan.setUpdatedAt(appliedAt);
        loanRepository.save(loan);

        if (disburse) {
            disbursementSeed(account, principal, loan.getApprovedAt(), loan.getLoanNumber());
            List<LoanPayment> schedule = buildSchedule(loan);
            loanPaymentRepository.saveAll(schedule);
            BigDecimal actualTotal = schedule.stream().map(LoanPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            loan.setTotalPayable(actualTotal);
            loan.setOutstandingBalance(actualTotal);
            loanRepository.save(loan);
        }
        return loan;
    }

    private List<LoanPayment> buildSchedule(Loan loan) {
        List<LoanPayment> schedule = new ArrayList<>();
        BigDecimal remaining = loan.getPrincipal();
        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 12, RoundingMode.HALF_UP);
        LocalDate baseDate = LocalDate.ofInstant(loan.getApprovedAt(), ZoneOffset.UTC);
        for (int i = 1; i <= loan.getTermMonths(); i++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComponent = i == loan.getTermMonths()
                    ? remaining : loan.getEmi().subtract(interest).max(BigDecimal.ZERO);
            if (principalComponent.compareTo(remaining) > 0) {
                principalComponent = remaining;
            }
            LoanPayment payment = new LoanPayment();
            payment.setLoan(loan);
            payment.setDueDate(baseDate.plusMonths(i));
            payment.setAmount(principalComponent.add(interest));
            payment.setPrincipalComponent(principalComponent);
            payment.setInterestComponent(interest);
            payment.setStatus(LoanPaymentStatus.PENDING);
            payment.setCreatedAt(loan.getApprovedAt());
            payment.setUpdatedAt(loan.getApprovedAt());
            schedule.add(payment);
            remaining = remaining.subtract(principalComponent);
        }
        return schedule;
    }

    private void seedCards(List<Customer> customers, List<Account> accounts) {
        for (int i = 0; i < 8; i++) {
            Card card = new Card();
            card.setCardReference("CARD" + String.format("%016d", 20260001L + i));
            card.setCustomer(customers.get(i));
            card.setAccount(accounts.get(i));
            card.setCardType(i == 6 ? CardType.CREDIT : CardType.DEBIT);
            card.setCardHolderName(customers.get(i).getUser().getFullName().toUpperCase(Locale.ROOT));
            card.setLastFour(String.format("%04d", 4100 + i));
            card.setExpiryMonth(1 + (i % 12));
            card.setExpiryYear(LocalDate.now().getYear() + 5);
            card.setNetwork(i % 3 == 0 ? "VISA" : (i % 3 == 1 ? "MASTERCARD" : "RUPAY"));
            card.setStatus(i == 5 ? CardStatus.BLOCKED : CardStatus.ACTIVE);
            card.setCreditLimit(i == 6 ? new BigDecimal("250000.00") : BigDecimal.ZERO);
            card.setOutstandingAmount(i == 6 ? new BigDecimal("18450.00") : BigDecimal.ZERO);
            card.setCreatedAt(Instant.now().minus(30 + i, ChronoUnit.DAYS));
            card.setUpdatedAt(card.getCreatedAt());
            cardRepository.save(card);
        }
    }

    private void seedNotifications(List<Customer> customers, User admin) {
        notification(customers.get(0).getUser(), "Welcome to Northstar Bank",
                "Your digital banking profile is ready. Explore accounts, transfers and cards.",
                NotificationType.SYSTEM, "/dashboard", false, 5);
        notification(customers.get(0).getUser(), "Deposit received",
                "Your salary deposit was credited successfully.", NotificationType.TRANSACTION,
                "/transactions", true, 2);
        notification(customers.get(0).getUser(), "Card issued",
                "Your new card is ready to use. You can manage it from the Cards page.",
                NotificationType.CARD, "/cards", false, 1);
        notification(customers.get(1).getUser(), "Loan disbursed",
                "Your home loan was approved and disbursed to your savings account.",
                NotificationType.LOAN, "/loans", false, 3);
        notification(customers.get(2).getUser(), "KYC pending",
                "Complete your KYC to unlock all banking services.", NotificationType.KYC,
                "/profile", false, 4);
        notification(admin, "Daily operations digest",
                "The banking platform processed transactions successfully. Review reports for details.",
                NotificationType.SYSTEM, "/reports", false, 1);
        notification(admin, "Loan approvals waiting",
                "New loan applications are awaiting credit review.", NotificationType.LOAN,
                "/loans", false, 2);
    }

    private void notification(User user, String title, String message, NotificationType type,
                              String link, boolean read, int daysAgo) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLink(link);
        notification.setRead(read);
        notification.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        notification.setUpdatedAt(notification.getCreatedAt());
        notificationRepository.save(notification);
    }

    private void seedAuditLogs() {
        String[][] logs = {
                {"admin@bank.com", "LOGIN", "User", "Successful login"},
                {"manager@bank.com", "LOAN_APPROVED", "Loan", "Loan LN2026000001 approved"},
                {"employee@bank.com", "DEPOSIT", "Account", "Branch deposit processed"},
                {"admin@bank.com", "BRANCH_CREATED", "Branch", "Branch BR003 created"},
                {"manager@bank.com", "KYC_VERIFIED", "Customer", "KYC verification completed"},
                {"system", "DAILY_INTEREST_ACCRUAL", "Account", "Scheduled job completed"},
                {"admin@bank.com", "REPORT_EXPORT", "Report", "Monthly report exported"},
                {"employee@bank.com", "CARD_BLOCKED", "Card", "Card ending 4105 blocked"}
        };
        for (int i = 0; i < logs.length; i++) {
            AuditLog audit = new AuditLog();
            audit.setUserEmail(logs[i][0]);
            audit.setAction(logs[i][1]);
            audit.setEntityName(logs[i][2]);
            audit.setDetails(logs[i][3]);
            audit.setStatus("SUCCESS");
            audit.setIpAddress("127.0.0.1");
            audit.setCreatedAt(Instant.now().minus(i + 1L, ChronoUnit.HOURS));
            audit.setUpdatedAt(audit.getCreatedAt());
            auditLogRepository.save(audit);
        }
    }

    private User createUser(String email, String firstName, String lastName, String password,
                            String phone, Role role, Instant createdAt) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(password));
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.addRole(role);
            user.setCreatedAt(createdAt);
            user.setUpdatedAt(createdAt);
            return userRepository.save(user);
        });
    }

    private Role role(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Missing role " + name));
    }

    private AccountType accountType(String code) {
        return accountTypeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalStateException("Missing account type " + code));
    }

    private void depositSeed(Account account, BigDecimal amount, Instant at, String description, String performedBy) {
        account.credit(amount);
        accountRepository.save(account);
        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        transaction(account, null, TransactionType.DEPOSIT, amount, account.getBalance(),
                description, group, reference, performedBy, at);
        ledger(group, reference, "CASH", LedgerEntryType.DEBIT, amount, "Cash received", at);
        ledger(group, reference, account.getAccountNumber(), LedgerEntryType.CREDIT, amount,
                "Deposit credited", at);
    }

    private void withdrawalSeed(Account account, BigDecimal amount, Instant at, String description, String performedBy) {
        account.debit(amount);
        accountRepository.save(account);
        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        transaction(account, null, TransactionType.WITHDRAWAL, amount, account.getBalance(),
                description, group, reference, performedBy, at);
        ledger(group, reference, account.getAccountNumber(), LedgerEntryType.DEBIT, amount,
                "Withdrawal debited", at);
        ledger(group, reference, "CASH", LedgerEntryType.CREDIT, amount, "Cash paid", at);
    }

    private void transferSeed(Account from, Account to, BigDecimal amount, Instant at) {
        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);
        String group = ReferenceGenerator.transactionGroup();
        String debitRef = ReferenceGenerator.transactionReference();
        String creditRef = ReferenceGenerator.transactionReference();
        transaction(from, to.getAccountNumber(), TransactionType.TRANSFER_DEBIT, amount,
                from.getBalance(), "Transfer to " + to.getAccountNumber(), group, debitRef,
                from.getCustomer().getUser().getEmail(), at);
        transaction(to, from.getAccountNumber(), TransactionType.TRANSFER_CREDIT, amount,
                to.getBalance(), "Transfer from " + from.getAccountNumber(), group, creditRef,
                from.getCustomer().getUser().getEmail(), at);
        ledger(group, debitRef, from.getAccountNumber(), LedgerEntryType.DEBIT, amount,
                "Transfer to " + to.getAccountNumber(), at);
        ledger(group, creditRef, to.getAccountNumber(), LedgerEntryType.CREDIT, amount,
                "Transfer from " + from.getAccountNumber(), at);
    }

    private void disbursementSeed(Account account, BigDecimal principal, Instant at, String loanNumber) {
        account.credit(principal);
        accountRepository.save(account);
        String group = ReferenceGenerator.transactionGroup();
        String reference = ReferenceGenerator.transactionReference();
        transaction(account, null, TransactionType.LOAN_DISBURSEMENT, principal, account.getBalance(),
                "Loan disbursement for " + loanNumber, group, reference, "manager@bank.com", at);
        ledger(group, reference, "LOAN_RECEIVABLE", LedgerEntryType.DEBIT, principal,
                "Loan principal disbursed", at);
        ledger(group, reference, account.getAccountNumber(), LedgerEntryType.CREDIT, principal,
                "Loan disbursement credited", at);
    }

    private void transaction(Account account, String counterparty, TransactionType type, BigDecimal amount,
                             BigDecimal balanceAfter, String description, String group, String reference,
                             String performedBy, Instant at) {
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
        transaction.setPerformedBy(performedBy);
        transaction.setCreatedAt(at);
        transaction.setUpdatedAt(at);
        transactionRepository.save(transaction);
    }

    private void ledger(String group, String reference, String accountNumber, LedgerEntryType type,
                        BigDecimal amount, String description, Instant at) {
        LedgerEntry entry = new LedgerEntry(group, reference, accountNumber, type, amount, description);
        entry.setCreatedAt(at);
        entry.setUpdatedAt(at);
        ledgerEntryRepository.save(entry);
    }

    private BigDecimal money(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

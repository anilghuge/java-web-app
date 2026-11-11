package com.banking.service.impl;

import com.banking.dto.AdminDtos;
import com.banking.dto.CommonDtos;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.DashboardDtos;
import com.banking.entity.Account;
import com.banking.entity.AuditLog;
import com.banking.entity.Branch;
import com.banking.entity.Customer;
import com.banking.entity.Employee;
import com.banking.entity.Role;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.LoanStatus;
import com.banking.enums.NotificationType;
import com.banking.enums.RoleName;
import com.banking.enums.TransactionType;
import com.banking.exception.ApiException;
import com.banking.mapper.AdminMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.AuditLogRepository;
import com.banking.repository.BranchRepository;
import com.banking.repository.CustomerRepository;
import com.banking.repository.EmployeeRepository;
import com.banking.repository.LoanRepository;
import com.banking.repository.RefreshTokenRepository;
import com.banking.repository.RoleRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AdminService;
import com.banking.service.AuditService;
import com.banking.service.DashboardService;
import com.banking.service.NotificationService;
import com.banking.util.PaginationUtil;
import com.banking.util.ReferenceGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DashboardService dashboardService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.BranchResponse> branches(boolean activeOnly) {
        List<Branch> branches = activeOnly
                ? branchRepository.findByActiveTrueOrderByName()
                : branchRepository.findAll().stream().sorted(Comparator.comparing(Branch::getName)).toList();
        return branches.stream().map(this::toBranchResponse).toList();
    }

    @Override
    @Transactional
    public AdminDtos.BranchResponse createBranch(AdminDtos.BranchRequest request) {
        if (branchRepository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("BRANCH_CODE_EXISTS", "A branch with this code already exists");
        }
        Branch branch = new Branch();
        applyBranch(branch, request);
        branchRepository.save(branch);
        auditService.log("BRANCH_CREATED", "Branch", branch.getId(), "Branch " + branch.getCode() + " created");
        return toBranchResponse(branch);
    }

    @Override
    @Transactional
    public AdminDtos.BranchResponse updateBranch(Long id, AdminDtos.BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Branch not found"));
        if (!branch.getCode().equalsIgnoreCase(request.code())
                && branchRepository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("BRANCH_CODE_EXISTS", "A branch with this code already exists");
        }
        applyBranch(branch, request);
        branchRepository.save(branch);
        auditService.log("BRANCH_UPDATED", "Branch", branch.getId(), "Branch " + branch.getCode() + " updated");
        return toBranchResponse(branch);
    }

    @Override
    @Transactional
    public AdminDtos.BranchResponse deactivateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Branch not found"));
        branch.setActive(false);
        branchRepository.save(branch);
        auditService.log("BRANCH_DEACTIVATED", "Branch", branch.getId(),
                "Branch " + branch.getCode() + " deactivated");
        return toBranchResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.EmployeeResponse> employees(String query, int page, int size) {
        Specification<Employee> spec = (root, cq, cb) -> {
            if (!StringUtils.hasText(query)) {
                return cb.conjunction();
            }
            Join<Employee, User> user = root.join("user");
            String like = "%" + query.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("employeeCode")), like),
                    cb.like(cb.lower(user.get("firstName")), like),
                    cb.like(cb.lower(user.get("lastName")), like),
                    cb.like(cb.lower(user.get("email")), like),
                    cb.like(cb.lower(root.get("designation")), like));
        };
        Page<Employee> employees = employeeRepository.findAll(spec,
                PaginationUtil.pageable(page, size, "createdAt", "desc"));
        return new PageResponse<>(
                employees.getContent().stream().map(AdminMapper::toEmployeeResponse).toList(),
                employees.getNumber(), employees.getSize(), employees.getTotalElements(),
                employees.getTotalPages(), employees.isLast());
    }

    @Override
    @Transactional
    public AdminDtos.EmployeeResponse createEmployee(AdminDtos.EmployeeRequest request) {
        validateEmployeeRole(request.role());
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_EXISTS", "A user with this email already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw ApiException.badRequest("PASSWORD_REQUIRED", "Password is required when creating an employee");
        }
        if (request.password().length() < 8) {
            throw ApiException.badRequest("WEAK_PASSWORD", "Password must be at least 8 characters");
        }
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Branch not found"));
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> ApiException.badRequest("ROLE_NOT_FOUND", "Role is not configured"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.addRole(role);
        userRepository.save(user);

        Employee employee = new Employee();
        employee.setEmployeeCode(generateUniqueEmployeeCode());
        employee.setUser(user);
        employee.setBranch(branch);
        employee.setDesignation(request.designation());
        employee.setJoiningDate(request.joiningDate());
        employee.setSalary(request.salary());
        employee.setActive(true);
        employeeRepository.save(employee);

        notificationService.notify(user, "Welcome to Northstar Bank",
                "Your employee account is active. Branch: " + branch.getName()
                        + ", designation: " + request.designation() + ".",
                NotificationType.SYSTEM, "/dashboard");
        auditService.log("EMPLOYEE_CREATED", "Employee", employee.getId(),
                "Employee " + employee.getEmployeeCode() + " created with role " + request.role());
        return AdminMapper.toEmployeeResponse(employee);
    }

    @Override
    @Transactional
    public AdminDtos.EmployeeResponse updateEmployee(Long id, AdminDtos.EmployeeRequest request) {
        validateEmployeeRole(request.role());
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("EMPLOYEE_NOT_FOUND", "Employee not found"));
        User user = employee.getUser();
        String email = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_EXISTS", "A user with this email already exists");
        }
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        if (StringUtils.hasText(request.password())) {
            if (request.password().length() < 8) {
                throw ApiException.badRequest("WEAK_PASSWORD", "Password must be at least 8 characters");
            }
            user.setPassword(passwordEncoder.encode(request.password()));
            refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        }
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> ApiException.badRequest("ROLE_NOT_FOUND", "Role is not configured"));
        user.getRoles().clear();
        user.addRole(role);
        userRepository.save(user);

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Branch not found"));
        employee.setBranch(branch);
        employee.setDesignation(request.designation());
        employee.setJoiningDate(request.joiningDate());
        employee.setSalary(request.salary());
        employeeRepository.save(employee);
        auditService.log("EMPLOYEE_UPDATED", "Employee", employee.getId(),
                "Employee " + employee.getEmployeeCode() + " updated");
        return AdminMapper.toEmployeeResponse(employee);
    }

    @Override
    @Transactional
    public AdminDtos.EmployeeResponse deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("EMPLOYEE_NOT_FOUND", "Employee not found"));
        employee.setActive(false);
        employee.getUser().setEnabled(false);
        userRepository.save(employee.getUser());
        refreshTokenRepository.revokeAllForUser(employee.getUser().getId(), Instant.now());
        employeeRepository.save(employee);
        auditService.log("EMPLOYEE_DEACTIVATED", "Employee", employee.getId(),
                "Employee " + employee.getEmployeeCode() + " deactivated");
        return AdminMapper.toEmployeeResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.RoleResponse> roles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(r -> r.getName().name()))
                .map(role -> AdminMapper.toRoleResponse(role, userRepository.countByRolesId(role.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.AuditLogResponse> auditLogs(String query, String action, String userEmail,
                                                              Instant from, Instant to, int page, int size) {
        Specification<AuditLog> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query)) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), like),
                        cb.like(cb.lower(root.get("userEmail")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("entityName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("details"), "")), like)));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(userEmail)) {
                predicates.add(cb.like(cb.lower(root.get("userEmail")), "%" + userEmail.trim().toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AuditLog> logs = auditLogRepository.findAll(spec,
                PaginationUtil.pageable(page, size, "createdAt", "desc"));
        return new PageResponse<>(
                logs.getContent().stream().map(AdminMapper::toAuditLog).toList(),
                logs.getNumber(), logs.getSize(), logs.getTotalElements(),
                logs.getTotalPages(), logs.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.ReportDashboard reports(Instant from, Instant to) {
        DateRange range = resolveRange(from, to);
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(range.from(), range.to());

        BigDecimal deposits = sum(transactions, TransactionType.DEPOSIT);
        BigDecimal withdrawals = sum(transactions, TransactionType.WITHDRAWAL);
        BigDecimal transfers = sum(transactions, TransactionType.TRANSFER_DEBIT);
        BigDecimal loanDisbursements = sum(transactions, TransactionType.LOAN_DISBURSEMENT);
        BigDecimal loanRepayments = sum(transactions, TransactionType.LOAN_REPAYMENT);
        long newCustomers = customerRepository.countByCreatedAtBetween(range.from(), range.to());
        long newAccounts = accountRepository.countByCreatedAtBetween(range.from(), range.to());
        BigDecimal totalBalance = accountRepository.sumTotalBalance();
        long accountCount = accountRepository.countByDeletedFalse();
        BigDecimal averageBalance = accountCount == 0 ? BigDecimal.ZERO
                : totalBalance.divide(BigDecimal.valueOf(accountCount), 2, RoundingMode.HALF_UP);

        Map<TransactionType, BigDecimal> mix = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            mix.merge(transaction.getType(), transaction.getAmount(), BigDecimal::add);
        }
        List<CommonDtos.NameValue> transactionMix = mix.entrySet().stream()
                .sorted(Map.Entry.<TransactionType, BigDecimal>comparingByValue().reversed())
                .map(e -> new CommonDtos.NameValue(e.getKey().name(), e.getValue()))
                .toList();

        AdminDtos.ReportSummary summary = new AdminDtos.ReportSummary(
                range.from(), range.to(), deposits, withdrawals, transfers, loanDisbursements,
                loanRepayments, averageBalance, newCustomers, newAccounts, transactions.size(),
                loanRepository.countByStatus(LoanStatus.PENDING), transactionMix);

        List<AdminDtos.CustomerGrowthPoint> growth = growthPoints();
        List<DashboardDtos.TransactionAnalyticsPoint> trend = dashboardService.transactionTrend(6);
        return new AdminDtos.ReportDashboard(summary, growth, trend);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsCsv(Instant from, Instant to) {
        DateRange range = resolveRange(from, to);
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(range.from(), range.to());
        StringBuilder csv = new StringBuilder();
        csv.append("Reference,Date,Account,Customer,Type,Status,Amount,BalanceAfter,Description,PerformedBy\n");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        for (Transaction transaction : transactions) {
            csv.append(escape(transaction.getReference())).append(',')
                    .append(escape(formatter.format(transaction.getCreatedAt()))).append(',')
                    .append(escape(transaction.getAccount().getAccountNumber())).append(',')
                    .append(escape(transaction.getAccount().getCustomer().getUser().getFullName())).append(',')
                    .append(escape(transaction.getType().name())).append(',')
                    .append(escape(transaction.getStatus().name())).append(',')
                    .append(transaction.getAmount().setScale(2, RoundingMode.HALF_UP)).append(',')
                    .append(transaction.getBalanceAfter().setScale(2, RoundingMode.HALF_UP)).append(',')
                    .append(escape(transaction.getDescription())).append(',')
                    .append(escape(transaction.getPerformedBy())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void applyBranch(Branch branch, AdminDtos.BranchRequest request) {
        branch.setCode(request.code().trim().toUpperCase());
        branch.setName(request.name().trim());
        branch.setAddress(request.address().trim());
        branch.setCity(request.city().trim());
        branch.setState(request.state().trim());
        branch.setPostalCode(request.postalCode().trim());
        branch.setPhone(request.phone().trim());
        branch.setEmail(request.email().trim().toLowerCase());
        branch.setIfscCode(request.ifscCode().trim().toUpperCase());
        branch.setActive(request.active());
    }

    private AdminDtos.BranchResponse toBranchResponse(Branch branch) {
        return AdminMapper.toBranchResponse(branch,
                (int) employeeRepository.countByBranchIdAndActiveTrue(branch.getId()),
                (int) accountRepository.countByBranchIdAndDeletedFalse(branch.getId()));
    }

    private void validateEmployeeRole(RoleName role) {
        if (role == RoleName.CUSTOMER) {
            throw ApiException.badRequest("INVALID_EMPLOYEE_ROLE", "Employees must have a staff role");
        }
        if (role == RoleName.ADMIN && !SecurityUtils.isAdmin()) {
            throw ApiException.forbidden("ADMIN_ROLE_REQUIRED", "Only administrators can assign the ADMIN role");
        }
    }

    private String generateUniqueEmployeeCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = ReferenceGenerator.employeeCode();
            if (!employeeRepository.existsByEmployeeCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique employee code");
    }

    private List<AdminDtos.CustomerGrowthPoint> growthPoints() {
        YearMonth start = YearMonth.now(ZoneOffset.UTC).minusMonths(5);
        Instant from = start.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<YearMonth, Long> customerCounts = new LinkedHashMap<>();
        Map<YearMonth, Long> accountCounts = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = start.plusMonths(i);
            customerCounts.put(month, 0L);
            accountCounts.put(month, 0L);
        }
        for (Customer customer : customerRepository.findAll()) {
            YearMonth month = YearMonth.from(customer.getCreatedAt().atZone(ZoneOffset.UTC));
            if (customerCounts.containsKey(month)) {
                customerCounts.merge(month, 1L, Long::sum);
            }
        }
        for (Account account : accountRepository.findAll()) {
            YearMonth month = YearMonth.from(account.getOpenedAt().atZone(ZoneOffset.UTC));
            if (accountCounts.containsKey(month)) {
                accountCounts.merge(month, 1L, Long::sum);
            }
        }
        List<AdminDtos.CustomerGrowthPoint> points = new ArrayList<>();
        customerCounts.forEach((month, customers) -> points.add(new AdminDtos.CustomerGrowthPoint(
                month.atDay(1).format(DateTimeFormatter.ofPattern("MMM yyyy")),
                customers, accountCounts.getOrDefault(month, 0L))));
        return points;
    }

    private DateRange resolveRange(Instant from, Instant to) {
        Instant rangeTo = to == null ? Instant.now() : to;
        Instant rangeFrom = from == null ? rangeTo.minus(30, ChronoUnit.DAYS) : from;
        if (rangeFrom.isAfter(rangeTo)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "From date must be before to date");
        }
        if (Duration.between(rangeFrom, rangeTo).toDays() > 366) {
            throw ApiException.badRequest("DATE_RANGE_TOO_LARGE", "Report range cannot exceed 366 days");
        }
        return new DateRange(rangeFrom, rangeTo);
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream().filter(t -> t.getType() == type)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")
                ? "\"" + escaped + "\"" : escaped;
    }

    private record DateRange(Instant from, Instant to) {
    }
}

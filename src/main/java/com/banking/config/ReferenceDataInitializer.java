package com.banking.config;

import com.banking.entity.AccountType;
import com.banking.entity.Permission;
import com.banking.entity.Role;
import com.banking.enums.RoleName;
import com.banking.repository.AccountTypeRepository;
import com.banking.repository.PermissionRepository;
import com.banking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Idempotently creates roles, permissions and account types required for the
 * application to operate. Runs before demo data seeding.
 */
@Component
@Profile("!test")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountTypeRepository accountTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permission> permissions = new LinkedHashMap<>();
        permission("CUSTOMER_READ", "View customer profiles", "Customers", permissions);
        permission("CUSTOMER_WRITE", "Create and update customers", "Customers", permissions);
        permission("KYC_APPROVE", "Approve or reject KYC", "Customers", permissions);
        permission("ACCOUNT_READ", "View bank accounts", "Accounts", permissions);
        permission("ACCOUNT_WRITE", "Open accounts and change status", "Accounts", permissions);
        permission("TRANSACTION_READ", "View transactions", "Transactions", permissions);
        permission("TRANSACTION_WRITE", "Perform deposits, withdrawals and transfers", "Transactions", permissions);
        permission("LOAN_READ", "View loans", "Loans", permissions);
        permission("LOAN_WRITE", "Apply for loans and repay", "Loans", permissions);
        permission("LOAN_APPROVE", "Approve or reject loans", "Loans", permissions);
        permission("CARD_READ", "View cards", "Cards", permissions);
        permission("CARD_WRITE", "Issue, block and unblock cards", "Cards", permissions);
        permission("BENEFICIARY_READ", "View beneficiaries", "Beneficiaries", permissions);
        permission("BENEFICIARY_WRITE", "Manage beneficiaries", "Beneficiaries", permissions);
        permission("REPORT_READ", "View management reports", "Reports", permissions);
        permission("AUDIT_READ", "View audit logs", "Audit", permissions);
        permission("EMPLOYEE_READ", "View employees", "Administration", permissions);
        permission("EMPLOYEE_WRITE", "Manage employees", "Administration", permissions);
        permission("BRANCH_READ", "View branches", "Administration", permissions);
        permission("BRANCH_WRITE", "Manage branches", "Administration", permissions);
        permission("ROLE_READ", "View roles and permissions", "Administration", permissions);
        permission("NOTIFICATION_READ", "View notifications", "Notifications", permissions);

        Map<RoleName, List<String>> grants = Map.of(
                RoleName.ADMIN, List.copyOf(permissions.keySet()),
                RoleName.MANAGER, List.of(
                        "CUSTOMER_READ", "CUSTOMER_WRITE", "KYC_APPROVE", "ACCOUNT_READ", "ACCOUNT_WRITE",
                        "TRANSACTION_READ", "TRANSACTION_WRITE", "LOAN_READ", "LOAN_APPROVE", "CARD_READ",
                        "CARD_WRITE", "BENEFICIARY_READ", "REPORT_READ", "AUDIT_READ", "EMPLOYEE_READ",
                        "BRANCH_READ", "NOTIFICATION_READ"),
                RoleName.BANK_EMPLOYEE, List.of(
                        "CUSTOMER_READ", "CUSTOMER_WRITE", "ACCOUNT_READ", "ACCOUNT_WRITE", "TRANSACTION_READ",
                        "TRANSACTION_WRITE", "LOAN_READ", "CARD_READ", "CARD_WRITE", "BENEFICIARY_READ",
                        "BRANCH_READ", "NOTIFICATION_READ"),
                RoleName.CUSTOMER, List.of(
                        "ACCOUNT_READ", "TRANSACTION_READ", "TRANSACTION_WRITE", "LOAN_READ", "LOAN_WRITE",
                        "CARD_READ", "CARD_WRITE", "BENEFICIARY_READ", "BENEFICIARY_WRITE", "NOTIFICATION_READ"));

        grants.forEach((roleName, permissionNames) -> {
            Role role = roleRepository.findByName(roleName).orElseGet(() -> {
                Role created = new Role();
                created.setName(roleName);
                created.setDescription(switch (roleName) {
                    case ADMIN -> "Full platform administration";
                    case MANAGER -> "Branch and credit operations management";
                    case BANK_EMPLOYEE -> "Day-to-day banking operations";
                    case CUSTOMER -> "Digital banking customer";
                });
                return created;
            });
            permissionNames.forEach(name -> role.addPermission(permissions.get(name)));
            roleRepository.save(role);
        });

        seedAccountType("SAVINGS", "Savings Account", "Everyday savings with interest",
                new BigDecimal("3.500"), new BigDecimal("500.00"), new BigDecimal("100000.00"));
        seedAccountType("CURRENT", "Current Account", "Business current account with no minimum balance",
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("250000.00"));
        seedAccountType("SALARY", "Salary Account", "Zero-balance salary account",
                new BigDecimal("2.500"), BigDecimal.ZERO, new BigDecimal("150000.00"));

        log.info("Reference data ready: {} roles, {} permissions, {} account types",
                roleRepository.count(), permissionRepository.count(), accountTypeRepository.count());
    }

    private void permission(String name, String description, String module, Map<String, Permission> target) {
        Permission permission = permissionRepository.findByName(name).orElseGet(() -> {
            Permission created = new Permission(name, description, module);
            return permissionRepository.save(created);
        });
        target.put(name, permission);
    }

    private void seedAccountType(String code, String name, String description,
                                 BigDecimal interestRate, BigDecimal minimumBalance, BigDecimal dailyLimit) {
        accountTypeRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            AccountType type = new AccountType();
            type.setCode(code);
            type.setName(name);
            type.setDescription(description);
            type.setInterestRate(interestRate);
            type.setMinimumBalance(minimumBalance);
            type.setDailyWithdrawalLimit(dailyLimit);
            type.setActive(true);
            return accountTypeRepository.save(type);
        });
    }
}

package com.banking.entity;

import com.banking.enums.LoanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_number", columnList = "loan_number", unique = true),
        @Index(name = "idx_loans_customer", columnList = "customer_id")
})
public class Loan extends BaseEntity {

    @Column(name = "loan_number", nullable = false, unique = true, length = 30)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "disbursement_account_id")
    private Account disbursementAccount;

    @Column(nullable = false, length = 40)
    private String loanType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private int termMonths;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal emi;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayable;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(nullable = false, length = 255)
    private String purpose;

    @Column(nullable = false)
    private Instant appliedAt = Instant.now();

    private Instant approvedAt;

    @Column(length = 160)
    private String approvedBy;

    @Column(length = 255)
    private String rejectionReason;

    private LocalDate nextDueDate;

    @Column(nullable = false)
    private int paidInstallments = 0;
}

package com.banking.entity;

import com.banking.enums.LoanPaymentStatus;
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
@Table(name = "loan_payments", indexes = @Index(name = "idx_loan_payments_loan", columnList = "loan_id"))
public class LoanPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalComponent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestComponent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanPaymentStatus status = LoanPaymentStatus.PENDING;

    private Instant paidAt;

    @Column(length = 40)
    private String transactionReference;
}

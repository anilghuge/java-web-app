package com.banking.repository;

import com.banking.entity.LoanPayment;
import com.banking.enums.LoanPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    List<LoanPayment> findByLoanIdOrderByDueDateAsc(Long loanId);

    Optional<LoanPayment> findFirstByLoanIdAndStatusOrderByDueDateAsc(Long loanId, LoanPaymentStatus status);

    long countByLoanIdAndStatus(Long loanId, LoanPaymentStatus status);
}

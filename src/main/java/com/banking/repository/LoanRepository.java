package com.banking.repository;

import com.banking.entity.Loan;
import com.banking.enums.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    Page<Loan> findByCustomerId(Long customerId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    long countByStatus(LoanStatus status);

    long countByStatusIn(Iterable<LoanStatus> statuses);

    long countByCustomerIdAndStatus(Long customerId, LoanStatus status);

    @Query("select coalesce(sum(l.outstandingBalance), 0) from Loan l where l.status in (com.banking.enums.LoanStatus.ACTIVE, com.banking.enums.LoanStatus.APPROVED)")
    BigDecimal sumOutstandingBalance();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(l) from Loan l where l.status in (com.banking.enums.LoanStatus.ACTIVE, com.banking.enums.LoanStatus.APPROVED)")
    long countActiveLoans();
}

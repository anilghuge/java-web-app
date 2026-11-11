package com.banking.repository;

import com.banking.entity.Account;
import com.banking.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByAccountNumberAndDeletedFalse(String accountNumber);

    Optional<Account> findByIdAndDeletedFalse(Long id);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerIdAndDeletedFalseOrderByOpenedAtDesc(Long customerId);

    Page<Account> findByCustomerIdAndDeletedFalse(Long customerId, Pageable pageable);

    Page<Account> findByDeletedFalse(Pageable pageable);

    long countByDeletedFalse();

    long countByCreatedAtBetween(java.time.Instant from, java.time.Instant to);

    long countByBranchIdAndDeletedFalse(Long branchId);

    long countByStatusAndDeletedFalse(AccountStatus status);

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.deleted = false and a.status <> com.banking.enums.AccountStatus.CLOSED")
    BigDecimal sumTotalBalance();

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.deleted = false and a.customer.id = :customerId and a.status <> com.banking.enums.AccountStatus.CLOSED")
    BigDecimal sumBalanceByCustomer(@Param("customerId") Long customerId);

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.deleted = false and a.customer.id = :customerId and a.accountType.code = :typeCode and a.status <> com.banking.enums.AccountStatus.CLOSED")
    BigDecimal sumBalanceByCustomerAndType(@Param("customerId") Long customerId, @Param("typeCode") String typeCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber in :numbers order by a.accountNumber")
    List<Account> findForUpdate(@Param("numbers") Collection<String> numbers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("select a.accountType.code, count(a), coalesce(sum(a.balance), 0) from Account a where a.deleted = false group by a.accountType.code")
    List<Object[]> statisticsByType();
}

package com.banking.repository;

import com.banking.entity.Transaction;
import com.banking.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    List<Transaction> findByTransactionGroupOrderByCreatedAtAsc(String transactionGroup);

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    Page<Transaction> findByAccountCustomerUserId(Long userId, Pageable pageable);

    List<Transaction> findTop8ByAccountCustomerUserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findTop10ByOrderByCreatedAtDesc();

    List<Transaction> findByCreatedAtBetween(Instant from, Instant to);

    List<Transaction> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long accountId, Instant from, Instant to);

    List<Transaction> findByAccountIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Long accountId, Instant from);

    long countByCreatedAtBetween(Instant from, Instant to);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.type = :type and t.createdAt between :from and :to")
    BigDecimal sumByTypeAndPeriod(@Param("type") TransactionType type, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.account.customer.id = :customerId and t.type = :type and t.createdAt between :from and :to")
    BigDecimal sumByCustomerAndTypeAndPeriod(@Param("customerId") Long customerId, @Param("type") TransactionType type,
                                             @Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.account.id = :accountId and t.type = :type and t.createdAt between :from and :to")
    BigDecimal sumByAccountAndTypeAndPeriod(@Param("accountId") Long accountId, @Param("type") TransactionType type,
                                            @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(t) from Transaction t where t.account.customer.id = :customerId")
    long countByCustomer(@Param("customerId") Long customerId);
}

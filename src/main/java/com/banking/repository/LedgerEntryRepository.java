package com.banking.repository;

import com.banking.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionGroupOrderByEntryTypeAsc(String transactionGroup);

    @Query("select coalesce(sum(l.amount), 0) from LedgerEntry l where l.entryType = com.banking.enums.LedgerEntryType.DEBIT and l.createdAt between :from and :to")
    BigDecimal sumDebits(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    @Query("select coalesce(sum(l.amount), 0) from LedgerEntry l where l.entryType = com.banking.enums.LedgerEntryType.CREDIT and l.createdAt between :from and :to")
    BigDecimal sumCredits(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);
}

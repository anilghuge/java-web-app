package com.banking.entity;

import com.banking.enums.LedgerEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_group", columnList = "transaction_group"),
        @Index(name = "idx_ledger_account", columnList = "account_number")
})
public class LedgerEntry extends BaseEntity {

    @Column(name = "transaction_group", nullable = false, length = 40)
    private String transactionGroup;

    @Column(name = "transaction_reference", nullable = false, length = 40)
    private String transactionReference;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    public LedgerEntry(String transactionGroup, String transactionReference, String accountNumber,
                       LedgerEntryType entryType, BigDecimal amount, String description) {
        this.transactionGroup = transactionGroup;
        this.transactionReference = transactionReference;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.description = description;
    }
}

package com.banking.mapper;

import com.banking.dto.TransactionDtos;
import com.banking.entity.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionDtos.TransactionResponse toResponse(Transaction t) {
        return new TransactionDtos.TransactionResponse(
                t.getId(),
                t.getReference(),
                t.getTransactionGroup(),
                t.getAccount().getId(),
                t.getAccount().getAccountNumber(),
                t.getAccount().getCustomer().getUser().getFullName(),
                t.getCounterpartyAccountNumber(),
                t.getType(),
                t.getStatus(),
                t.getAmount(),
                t.getBalanceAfter(),
                t.getDescription(),
                t.getPerformedBy(),
                t.getCreatedAt());
    }
}

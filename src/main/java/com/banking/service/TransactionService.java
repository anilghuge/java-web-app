package com.banking.service;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.TransactionDtos;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;

import java.time.Instant;

public interface TransactionService {

    TransactionDtos.TransactionResponse deposit(TransactionDtos.DepositRequest request);

    TransactionDtos.TransactionResponse withdraw(TransactionDtos.WithdrawRequest request);

    TransactionDtos.TransferResponse transfer(TransactionDtos.TransferRequest request, String idempotencyKey);

    PageResponse<TransactionDtos.TransactionResponse> list(Long accountId, Long customerId,
                                                           TransactionType type, TransactionStatus status,
                                                           Instant from, Instant to, String query,
                                                           int page, int size, String sortBy, String sortDir);

    TransactionDtos.TransactionResponse get(Long id);

    PageResponse<TransactionDtos.TransactionResponse> byAccount(Long accountId, int page, int size);

    TransactionDtos.StatementResponse statement(String accountNumber, Instant from, Instant to);

    byte[] statementPdf(String accountNumber, Instant from, Instant to);
}

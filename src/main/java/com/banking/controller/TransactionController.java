package com.banking.controller;

import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.TransactionDtos;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Deposits, withdrawals, atomic fund transfers and statements")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit money (customer's own account or any account for staff)")
    public ApiResponse<TransactionDtos.TransactionResponse> deposit(
            @Valid @RequestBody TransactionDtos.DepositRequest request) {
        return ApiResponse.ok("Deposit completed successfully", transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw money with balance, minimum balance and daily-limit checks")
    public ApiResponse<TransactionDtos.TransactionResponse> withdraw(
            @Valid @RequestBody TransactionDtos.WithdrawRequest request) {
        return ApiResponse.ok("Withdrawal completed successfully", transactionService.withdraw(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds atomically between accounts",
            description = "Requires X-Idempotency-Key. Replaying a completed key returns the original transfer. " +
                    "Concurrent duplicate keys cannot debit an account twice.")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true,
            description = "Client-generated unique key (UUID recommended)")
    public ApiResponse<TransactionDtos.TransferResponse> transfer(
            @Valid @RequestBody TransactionDtos.TransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok("Transfer completed successfully",
                transactionService.transfer(request, idempotencyKey));
    }

    @GetMapping
    @Operation(summary = "List transactions with filters and pagination")
    public ApiResponse<PageResponse<TransactionDtos.TransactionResponse>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.ok(transactionService.list(accountId, customerId, type, status, from, to,
                query, page, size, sortBy, sortDir));
    }

    @GetMapping("/statement")
    @Operation(summary = "Generate a JSON account statement for a date range")
    public ApiResponse<TransactionDtos.StatementResponse> statement(
            @RequestParam String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(transactionService.statement(accountNumber, from, to));
    }

    @GetMapping(value = "/statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download a PDF account statement")
    public ResponseEntity<byte[]> statementPdf(
            @RequestParam String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        byte[] pdf = transactionService.statementPdf(accountNumber, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"statement-" + accountNumber + "-"
                                + LocalDate.now() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "List transactions for an account")
    public ApiResponse<PageResponse<TransactionDtos.TransactionResponse>> byAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(transactionService.byAccount(accountId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by id")
    public ApiResponse<TransactionDtos.TransactionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(transactionService.get(id));
    }
}

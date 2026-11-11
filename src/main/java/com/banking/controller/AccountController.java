package com.banking.controller;

import com.banking.dto.AccountDtos;
import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.enums.AccountStatus;
import com.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Account opening, balances, status management and reference data")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a savings/current account for the authenticated customer or a supplied customer")
    public ResponseEntity<ApiResponse<AccountDtos.AccountResponse>> open(
            @Valid @RequestBody AccountDtos.OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account opened", accountService.open(request)));
    }

    @GetMapping
    @Operation(summary = "List accounts (all accounts for staff, own accounts for customers)")
    public ApiResponse<PageResponse<AccountDtos.AccountResponse>> list(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "openedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.ok(accountService.list(status, query, page, size, sortBy, sortDir));
    }

    @GetMapping("/types")
    @Operation(summary = "List active account types")
    public ApiResponse<List<AccountDtos.AccountTypeResponse>> accountTypes() {
        return ApiResponse.ok(accountService.accountTypes());
    }

    @GetMapping("/branches")
    @Operation(summary = "List active branches for account opening")
    public ApiResponse<List<AccountDtos.BranchOption>> branches() {
        return ApiResponse.ok(accountService.branches());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List accounts belonging to a customer")
    public ApiResponse<PageResponse<AccountDtos.AccountResponse>> byCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(accountService.byCustomer(customerId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an account by id")
    public ApiResponse<AccountDtos.AccountResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(accountService.get(id));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get current and available account balance")
    public ApiResponse<AccountDtos.AccountBalanceResponse> balance(@PathVariable Long id) {
        return ApiResponse.ok(accountService.balance(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "Update account status (staff only)")
    public ApiResponse<AccountDtos.AccountResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountDtos.UpdateAccountStatusRequest request) {
        return ApiResponse.ok("Account status updated", accountService.updateStatus(id, request));
    }
}

package com.banking.controller;

import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.LoanDtos;
import com.banking.enums.LoanStatus;
import com.banking.service.LoanService;
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

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Loans", description = "Loan applications, underwriting, EMI schedules and repayments")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Apply for a loan", description = "Supports PERSONAL, HOME, AUTO, EDUCATION and BUSINESS loans.")
    public ResponseEntity<ApiResponse<LoanDtos.LoanResponse>> apply(
            @Valid @RequestBody LoanDtos.LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Loan application submitted", loanService.apply(request)));
    }

    @GetMapping
    @Operation(summary = "List loans (own loans for customers, all loans for staff)")
    public ApiResponse<PageResponse<LoanDtos.LoanResponse>> list(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.ok(loanService.list(status, page, size, sortBy, sortDir));
    }

    @GetMapping("/emi-calculator")
    @Operation(summary = "Calculate EMI for a proposed loan")
    public ApiResponse<LoanDtos.EmiCalculationResponse> calculateEmi(
            @RequestParam java.math.BigDecimal principal,
            @RequestParam java.math.BigDecimal annualInterestRate,
            @RequestParam Integer termMonths) {
        return ApiResponse.ok(loanService.calculateEmi(
                new LoanDtos.EmiCalculationRequest(principal, annualInterestRate, termMonths)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan details")
    public ApiResponse<LoanDtos.LoanResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(loanService.get(id));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Get the loan repayment schedule")
    public ApiResponse<LoanDtos.LoanScheduleResponse> schedule(@PathVariable Long id) {
        return ApiResponse.ok(loanService.schedule(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Approve and disburse a pending loan")
    public ApiResponse<LoanDtos.LoanResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) LoanDtos.LoanDecisionRequest request) {
        return ApiResponse.ok("Loan approved and disbursed",
                loanService.approve(id, request == null ? new LoanDtos.LoanDecisionRequest(null) : request));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Reject a pending loan application")
    public ApiResponse<LoanDtos.LoanResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody LoanDtos.LoanRejectionRequest request) {
        return ApiResponse.ok("Loan rejected", loanService.reject(id, request));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Repay one or more EMIs, or settle the full outstanding balance")
    public ApiResponse<LoanDtos.LoanPaymentResponse> pay(
            @PathVariable Long id,
            @Valid @RequestBody LoanDtos.LoanPaymentRequest request) {
        return ApiResponse.ok("Loan repayment completed", loanService.pay(id, request));
    }
}

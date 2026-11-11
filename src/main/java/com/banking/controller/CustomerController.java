package com.banking.controller;

import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.MessageResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.CustomerDtos;
import com.banking.enums.KycStatus;
import com.banking.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customers", description = "Customer onboarding, profile, search and KYC workflows")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "Create a customer (staff only)")
    public ResponseEntity<ApiResponse<CustomerDtos.CustomerResponse>> create(
            @Valid @RequestBody CustomerDtos.CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer created", customerService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "List or filter customers with pagination")
    public ApiResponse<PageResponse<CustomerDtos.CustomerResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) KycStatus kycStatus,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.ok(customerService.list(query, kycStatus, city, page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "Search customers by name, email, phone or customer number")
    public ApiResponse<PageResponse<CustomerDtos.CustomerSummary>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(customerService.search(query, page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated customer's profile")
    public ApiResponse<CustomerDtos.CustomerResponse> me() {
        return ApiResponse.ok(customerService.me());
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated customer's profile")
    public ApiResponse<CustomerDtos.CustomerResponse> updateMe(
            @Valid @RequestBody CustomerDtos.UpdateCustomerRequest request) {
        return ApiResponse.ok("Profile updated", customerService.updateMe(request));
    }

    @PostMapping("/me/kyc")
    @Operation(summary = "Submit KYC details for verification")
    public ApiResponse<CustomerDtos.CustomerResponse> submitKyc(
            @Valid @RequestBody CustomerDtos.KycSubmissionRequest request) {
        return ApiResponse.ok("KYC submitted for verification", customerService.submitKyc(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer by id (customers may only read their own profile)")
    public ApiResponse<CustomerDtos.CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "Update a customer (staff only)")
    public ApiResponse<CustomerDtos.CustomerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDtos.UpdateCustomerRequest request) {
        return ApiResponse.ok("Customer updated", customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a customer, close zero-balance accounts and disable login")
    public ApiResponse<MessageResponse> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok("Customer deleted", new MessageResponse("Customer deleted"));
    }

    @PostMapping("/{id}/kyc-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BANK_EMPLOYEE')")
    @Operation(summary = "Verify or reject a customer's KYC submission")
    public ApiResponse<CustomerDtos.CustomerResponse> decideKyc(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDtos.KycDecisionRequest request) {
        return ApiResponse.ok("KYC decision recorded", customerService.decideKyc(id, request));
    }
}

package com.banking.controller;

import com.banking.dto.BeneficiaryDtos;
import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.MessageResponse;
import com.banking.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Beneficiaries", description = "Customer-owned payee management")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping
    @Operation(summary = "List the authenticated customer's beneficiaries")
    public ApiResponse<List<BeneficiaryDtos.BeneficiaryResponse>> list() {
        return ApiResponse.ok(beneficiaryService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a beneficiary")
    public ApiResponse<BeneficiaryDtos.BeneficiaryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(beneficiaryService.get(id));
    }

    @PostMapping
    @Operation(summary = "Add a beneficiary")
    public ResponseEntity<ApiResponse<BeneficiaryDtos.BeneficiaryResponse>> create(
            @Valid @RequestBody BeneficiaryDtos.BeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Beneficiary added", beneficiaryService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a beneficiary")
    public ApiResponse<BeneficiaryDtos.BeneficiaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BeneficiaryDtos.BeneficiaryRequest request) {
        return ApiResponse.ok("Beneficiary updated", beneficiaryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a beneficiary (soft delete)")
    public ApiResponse<MessageResponse> delete(@PathVariable Long id) {
        beneficiaryService.delete(id);
        return ApiResponse.ok("Beneficiary removed", new MessageResponse("Beneficiary removed"));
    }
}

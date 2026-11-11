package com.banking.controller;

import com.banking.dto.AdminDtos;
import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.dto.CommonDtos.PageResponse;
import com.banking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration", description = "Branches, employees, roles, audit logs and management reports")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/branches")
    @Operation(summary = "List branches with employee and account counts")
    public ApiResponse<List<AdminDtos.BranchResponse>> branches(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.ok(adminService.branches(activeOnly));
    }

    @PostMapping("/branches")
    @Operation(summary = "Create a branch")
    public ApiResponse<AdminDtos.BranchResponse> createBranch(
            @Valid @RequestBody AdminDtos.BranchRequest request) {
        return ApiResponse.ok("Branch created", adminService.createBranch(request));
    }

    @PutMapping("/branches/{id}")
    @Operation(summary = "Update a branch")
    public ApiResponse<AdminDtos.BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.BranchRequest request) {
        return ApiResponse.ok("Branch updated", adminService.updateBranch(id, request));
    }

    @PutMapping("/branches/{id}/deactivate")
    @Operation(summary = "Deactivate a branch")
    public ApiResponse<AdminDtos.BranchResponse> deactivateBranch(@PathVariable Long id) {
        return ApiResponse.ok("Branch deactivated", adminService.deactivateBranch(id));
    }

    @GetMapping("/employees")
    @Operation(summary = "List employees with search and pagination")
    public ApiResponse<PageResponse<AdminDtos.EmployeeResponse>> employees(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.employees(query, page, size));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an employee user (admin only)")
    public ApiResponse<AdminDtos.EmployeeResponse> createEmployee(
            @Valid @RequestBody AdminDtos.EmployeeRequest request) {
        return ApiResponse.ok("Employee created", adminService.createEmployee(request));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an employee (admin only)")
    public ApiResponse<AdminDtos.EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.EmployeeRequest request) {
        return ApiResponse.ok("Employee updated", adminService.updateEmployee(id, request));
    }

    @PutMapping("/employees/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an employee and revoke their sessions (admin only)")
    public ApiResponse<AdminDtos.EmployeeResponse> deactivateEmployee(@PathVariable Long id) {
        return ApiResponse.ok("Employee deactivated", adminService.deactivateEmployee(id));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List roles and their permissions (admin only)")
    public ApiResponse<List<AdminDtos.RoleResponse>> roles() {
        return ApiResponse.ok(adminService.roles());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Search the immutable audit trail")
    public ApiResponse<PageResponse<AdminDtos.AuditLogResponse>> auditLogs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.auditLogs(query, action, userEmail, from, to, page, size));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get management report summary, customer growth and transaction trend")
    public ApiResponse<AdminDtos.ReportDashboard> reports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(adminService.reports(from, to));
    }

    @GetMapping(value = "/reports/export", produces = "text/csv")
    @Operation(summary = "Export transactions in the selected period as CSV")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        byte[] csv = adminService.exportTransactionsCsv(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transactions-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}

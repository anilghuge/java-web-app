package com.banking.service;

import com.banking.dto.AdminDtos;
import com.banking.dto.CommonDtos.PageResponse;

import java.time.Instant;
import java.util.List;

public interface AdminService {

    List<AdminDtos.BranchResponse> branches(boolean activeOnly);

    AdminDtos.BranchResponse createBranch(AdminDtos.BranchRequest request);

    AdminDtos.BranchResponse updateBranch(Long id, AdminDtos.BranchRequest request);

    AdminDtos.BranchResponse deactivateBranch(Long id);

    PageResponse<AdminDtos.EmployeeResponse> employees(String query, int page, int size);

    AdminDtos.EmployeeResponse createEmployee(AdminDtos.EmployeeRequest request);

    AdminDtos.EmployeeResponse updateEmployee(Long id, AdminDtos.EmployeeRequest request);

    AdminDtos.EmployeeResponse deactivateEmployee(Long id);

    List<AdminDtos.RoleResponse> roles();

    PageResponse<AdminDtos.AuditLogResponse> auditLogs(String query, String action, String userEmail,
                                                       Instant from, Instant to, int page, int size);

    AdminDtos.ReportDashboard reports(Instant from, Instant to);

    byte[] exportTransactionsCsv(Instant from, Instant to);
}

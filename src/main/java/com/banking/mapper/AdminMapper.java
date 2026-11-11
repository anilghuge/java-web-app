package com.banking.mapper;

import com.banking.dto.AdminDtos;
import com.banking.entity.AuditLog;
import com.banking.entity.Branch;
import com.banking.entity.Employee;
import com.banking.entity.Notification;
import com.banking.entity.Permission;
import com.banking.entity.Role;

public final class AdminMapper {

    private AdminMapper() {
    }

    public static AdminDtos.BranchResponse toBranchResponse(Branch b, int employeeCount, int accountCount) {
        return new AdminDtos.BranchResponse(
                b.getId(), b.getCode(), b.getName(), b.getAddress(), b.getCity(), b.getState(),
                b.getPostalCode(), b.getPhone(), b.getEmail(), b.getIfscCode(), b.isActive(),
                employeeCount, accountCount, b.getCreatedAt());
    }

    public static AdminDtos.EmployeeResponse toEmployeeResponse(Employee e) {
        return new AdminDtos.EmployeeResponse(
                e.getId(), e.getEmployeeCode(), e.getUser().getId(),
                e.getUser().getFirstName(), e.getUser().getLastName(), e.getUser().getEmail(),
                e.getUser().getPhone(), e.getBranch().getId(), e.getBranch().getName(),
                e.getBranch().getCode(), e.getDesignation(), e.getJoiningDate(), e.getSalary(),
                e.isActive(), e.getUser().getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
                e.getCreatedAt());
    }

    public static AdminDtos.PermissionResponse toPermission(Permission p) {
        return new AdminDtos.PermissionResponse(p.getId(), p.getName(), p.getDescription(), p.getModule());
    }

    public static AdminDtos.RoleResponse toRoleResponse(Role r, long userCount) {
        return new AdminDtos.RoleResponse(
                r.getId(), r.getName().name(), r.getDescription(),
                r.getPermissions().stream().map(AdminMapper::toPermission)
                        .sorted(java.util.Comparator.comparing(AdminDtos.PermissionResponse::name))
                        .toList(),
                userCount);
    }

    public static AdminDtos.NotificationResponse toNotification(Notification n) {
        return new AdminDtos.NotificationResponse(
                n.getId(), n.getTitle(), n.getMessage(), n.getType().name(), n.isRead(),
                n.getLink(), n.getCreatedAt());
    }

    public static AdminDtos.AuditLogResponse toAuditLog(AuditLog a) {
        return new AdminDtos.AuditLogResponse(
                a.getId(), a.getUserEmail(), a.getAction(), a.getEntityName(), a.getEntityId(),
                a.getDetails(), a.getIpAddress(), a.getStatus(), a.getCreatedAt());
    }
}

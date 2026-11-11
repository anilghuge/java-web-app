package com.banking.service;

import com.banking.entity.AuditLog;
import com.banking.repository.AuditLogRepository;
import com.banking.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, String entityName, Object entityId, String details) {
        log(action, entityName, entityId, details, "SUCCESS");
    }

    public void log(String action, String entityName, Object entityId, String details, String status) {
        logAs(SecurityUtils.currentEmail() == null ? "system" : SecurityUtils.currentEmail(),
                action, entityName, entityId, details, status);
    }

    public void logAs(String email, String action, String entityName, Object entityId,
                      String details, String status) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserEmail(email == null || email.isBlank() ? "system" : email);
        auditLog.setAction(action);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId == null ? null : String.valueOf(entityId));
        auditLog.setDetails(details);
        auditLog.setStatus(status);
        auditLog.setIpAddress(resolveIp());
        auditLogRepository.save(auditLog);
    }

    private String resolveIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}

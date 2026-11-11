package com.banking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_email"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
public class AuditLog extends BaseEntity {

    @Column(name = "user_email", nullable = false, length = 160)
    private String userEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_name", length = 80)
    private String entityName;

    @Column(name = "entity_id", length = 40)
    private String entityId;

    @Column(length = 1000)
    private String details;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";
}

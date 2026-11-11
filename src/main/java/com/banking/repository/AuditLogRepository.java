package com.banking.repository;

import com.banking.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select a from AuditLog a where
                lower(a.userEmail) like lower(concat('%', :q, '%')) or
                lower(a.action) like lower(concat('%', :q, '%')) or
                lower(coalesce(a.entityName, '')) like lower(concat('%', :q, '%'))
            """)
    Page<AuditLog> search(@Param("q") String query, Pageable pageable);

    long countByCreatedAtBetween(Instant from, Instant to);
}

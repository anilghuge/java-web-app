package com.banking.repository;

import com.banking.entity.Customer;
import com.banking.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByUserEmailIgnoreCase(String email);

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByCustomerNumber(String customerNumber);

    Page<Customer> findByDeletedFalse(Pageable pageable);

    long countByDeletedFalse();

    long countByKycStatusAndDeletedFalse(KycStatus status);

    long countByCreatedAtBetween(java.time.Instant from, java.time.Instant to);

    @Query("""
            select c from Customer c
            where c.deleted = false and (
                lower(c.customerNumber) like lower(concat('%', :q, '%')) or
                lower(c.user.firstName) like lower(concat('%', :q, '%')) or
                lower(c.user.lastName) like lower(concat('%', :q, '%')) or
                lower(c.user.email) like lower(concat('%', :q, '%')) or
                lower(c.user.phone) like lower(concat('%', :q, '%'))
            )
            """)
    Page<Customer> search(@Param("q") String query, Pageable pageable);
}

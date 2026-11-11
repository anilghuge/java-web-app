package com.banking.repository;

import com.banking.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByCustomerIdAndDeletedFalseOrderByNameAsc(Long customerId);

    Optional<Beneficiary> findByIdAndCustomerIdAndDeletedFalse(Long id, Long customerId);

    Optional<Beneficiary> findByIdAndDeletedFalse(Long id);

    long countByCustomerIdAndDeletedFalse(Long customerId);

    boolean existsByCustomerIdAndAccountNumberAndDeletedFalse(Long customerId, String accountNumber);
}

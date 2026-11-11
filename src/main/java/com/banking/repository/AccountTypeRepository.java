package com.banking.repository;

import com.banking.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {

    Optional<AccountType> findByCodeIgnoreCase(String code);

    List<AccountType> findByActiveTrueOrderByName();
}

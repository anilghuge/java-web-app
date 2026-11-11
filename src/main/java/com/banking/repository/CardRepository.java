package com.banking.repository;

import com.banking.entity.Card;
import com.banking.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Card> findAllByOrderByCreatedAtDesc();

    Optional<Card> findByIdAndCustomerId(Long id, Long customerId);

    long countByStatus(CardStatus status);

    boolean existsByAccountIdAndCardType(Long accountId, com.banking.enums.CardType cardType);
}

package com.banking.dto;

import com.banking.enums.CardStatus;
import com.banking.enums.CardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class CardDtos {

    private CardDtos() {
    }

    public record CardRequest(
            @NotBlank String accountNumber,
            @NotNull CardType cardType,
            @Size(max = 120) String cardHolderName,
            @DecimalMin("0.0") BigDecimal creditLimit) {
    }

    public record CardResponse(
            Long id,
            String cardReference,
            Long customerId,
            String customerName,
            String accountNumber,
            CardType cardType,
            String cardHolderName,
            String maskedNumber,
            String lastFour,
            int expiryMonth,
            int expiryYear,
            String network,
            CardStatus status,
            BigDecimal creditLimit,
            BigDecimal outstandingAmount,
            Instant createdAt) {
    }
}

package com.banking.mapper;

import com.banking.dto.CardDtos;
import com.banking.entity.Card;

public final class CardMapper {

    private CardMapper() {
    }

    public static CardDtos.CardResponse toResponse(Card c) {
        return new CardDtos.CardResponse(
                c.getId(),
                c.getCardReference(),
                c.getCustomer().getId(),
                c.getCustomer().getUser().getFullName(),
                c.getAccount() != null ? c.getAccount().getAccountNumber() : null,
                c.getCardType(),
                c.getCardHolderName(),
                "**** **** **** " + c.getLastFour(),
                c.getLastFour(),
                c.getExpiryMonth(),
                c.getExpiryYear(),
                c.getNetwork(),
                c.getStatus(),
                c.getCreditLimit(),
                c.getOutstandingAmount(),
                c.getCreatedAt());
    }
}

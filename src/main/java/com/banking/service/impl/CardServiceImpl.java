package com.banking.service.impl;

import com.banking.dto.CardDtos;
import com.banking.entity.Account;
import com.banking.entity.Card;
import com.banking.entity.Customer;
import com.banking.enums.AccountStatus;
import com.banking.enums.CardStatus;
import com.banking.enums.CardType;
import com.banking.enums.NotificationType;
import com.banking.exception.ApiException;
import com.banking.mapper.CardMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.CardRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AuditService;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.CardService;
import com.banking.service.NotificationService;
import com.banking.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public CardDtos.CardResponse request(CardDtos.CardRequest request) {
        Account account = accountRepository.findByAccountNumberAndDeletedFalse(request.accountNumber())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        Customer customer;
        if (SecurityUtils.isStaff()) {
            customer = account.getCustomer();
        } else {
            customer = authenticatedUserService.customer();
            if (!account.getCustomer().getId().equals(customer.getId())) {
                throw ApiException.forbidden("ACCOUNT_ACCESS_DENIED", "You can only request cards for your own account");
            }
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("ACCOUNT_NOT_ACTIVE", "Cards can only be issued on active accounts");
        }
        if (cardRepository.existsByAccountIdAndCardType(account.getId(), request.cardType())) {
            throw ApiException.conflict("CARD_ALREADY_EXISTS",
                    "A " + request.cardType() + " card already exists for this account");
        }

        Card card = new Card();
        card.setCardReference(ReferenceGenerator.cardReference());
        card.setCustomer(customer);
        card.setAccount(account);
        card.setCardType(request.cardType());
        card.setCardHolderName(request.cardHolderName() == null || request.cardHolderName().isBlank()
                ? customer.getUser().getFullName().toUpperCase()
                : request.cardHolderName().trim().toUpperCase());
        card.setLastFour(ReferenceGenerator.lastFour());
        LocalDate expiry = LocalDate.now().plusYears(5);
        card.setExpiryMonth(expiry.getMonthValue());
        card.setExpiryYear(expiry.getYear());
        card.setNetwork("VISA");
        card.setStatus(CardStatus.ACTIVE);
        card.setCreditLimit(request.cardType() == CardType.CREDIT
                ? (request.creditLimit() == null ? new BigDecimal("100000") : request.creditLimit())
                : BigDecimal.ZERO);
        card.setOutstandingAmount(BigDecimal.ZERO);
        cardRepository.save(card);

        notificationService.notify(customer.getUser(), "New card issued",
                "Your " + request.cardType() + " card ending " + card.getLastFour()
                        + " is active. Manage it from the Cards page.",
                NotificationType.CARD, "/cards");
        auditService.log("CARD_ISSUED", "Card", card.getId(),
                "Card " + card.getCardReference() + " issued for account " + account.getAccountNumber());
        return CardMapper.toResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardDtos.CardResponse> list() {
        List<Card> cards;
        if (SecurityUtils.isStaff()) {
            cards = cardRepository.findAllByOrderByCreatedAtDesc();
        } else {
            cards = cardRepository.findByCustomerIdOrderByCreatedAtDesc(
                    authenticatedUserService.customer().getId());
        }
        return cards.stream()
                .sorted(Comparator.comparing(Card::getCreatedAt).reversed())
                .map(CardMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardDtos.CardResponse get(Long id) {
        return CardMapper.toResponse(loadAccessibleCard(id));
    }

    @Override
    @Transactional
    public CardDtos.CardResponse block(Long id) {
        Card card = loadAccessibleCard(id);
        if (card.getStatus() == CardStatus.BLOCKED) {
            return CardMapper.toResponse(card);
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw ApiException.conflict("CARD_NOT_ACTIVE", "Only active cards can be blocked");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        notificationService.notify(card.getCustomer().getUser(), "Card blocked",
                "Card ending " + card.getLastFour() + " has been blocked.",
                NotificationType.CARD, "/cards");
        auditService.log("CARD_BLOCKED", "Card", card.getId(), "Card " + card.getCardReference() + " blocked");
        return CardMapper.toResponse(card);
    }

    @Override
    @Transactional
    public CardDtos.CardResponse unblock(Long id) {
        Card card = loadAccessibleCard(id);
        if (card.getStatus() == CardStatus.ACTIVE) {
            return CardMapper.toResponse(card);
        }
        if (card.getStatus() != CardStatus.BLOCKED) {
            throw ApiException.conflict("CARD_CANNOT_BE_UNBLOCKED", "Only blocked cards can be unblocked");
        }
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
        notificationService.notify(card.getCustomer().getUser(), "Card unblocked",
                "Card ending " + card.getLastFour() + " is active again.",
                NotificationType.CARD, "/cards");
        auditService.log("CARD_UNBLOCKED", "Card", card.getId(), "Card " + card.getCardReference() + " unblocked");
        return CardMapper.toResponse(card);
    }

    private Card loadAccessibleCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CARD_NOT_FOUND", "Card not found"));
        if (SecurityUtils.isStaff()) {
            return card;
        }
        Customer customer = authenticatedUserService.customer();
        if (!card.getCustomer().getId().equals(customer.getId())) {
            throw ApiException.forbidden("CARD_ACCESS_DENIED", "You can only access your own cards");
        }
        return card;
    }
}

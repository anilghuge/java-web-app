package com.banking.service;

import com.banking.dto.CardDtos;

import java.util.List;

public interface CardService {

    CardDtos.CardResponse request(CardDtos.CardRequest request);

    List<CardDtos.CardResponse> list();

    CardDtos.CardResponse get(Long id);

    CardDtos.CardResponse block(Long id);

    CardDtos.CardResponse unblock(Long id);
}

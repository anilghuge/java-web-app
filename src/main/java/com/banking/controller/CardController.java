package com.banking.controller;

import com.banking.dto.CardDtos;
import com.banking.dto.CommonDtos.ApiResponse;
import com.banking.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cards", description = "Debit and credit card issuance and lifecycle management")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "List cards (own cards for customers, all cards for staff)")
    public ApiResponse<List<CardDtos.CardResponse>> list() {
        return ApiResponse.ok(cardService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card details (PAN is always masked)")
    public ApiResponse<CardDtos.CardResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(cardService.get(id));
    }

    @PostMapping
    @Operation(summary = "Request a debit or credit card")
    public ResponseEntity<ApiResponse<CardDtos.CardResponse>> request(
            @Valid @RequestBody CardDtos.CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Card issued", cardService.request(request)));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block a card immediately")
    public ApiResponse<CardDtos.CardResponse> block(@PathVariable Long id) {
        return ApiResponse.ok("Card blocked", cardService.block(id));
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock a blocked card")
    public ApiResponse<CardDtos.CardResponse> unblock(@PathVariable Long id) {
        return ApiResponse.ok("Card unblocked", cardService.unblock(id));
    }
}

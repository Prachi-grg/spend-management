package com.spendmanagement.api.controller;

import com.spendmanagement.api.dto.CardRequest;
import com.spendmanagement.api.dto.CardResponse;
import com.spendmanagement.api.dto.SpendingLimitRequest;
import com.spendmanagement.domain.Card;
import com.spendmanagement.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(@Valid @RequestBody CardRequest request) {
        return CardResponse.from(cardService.createCard(request));
    }

    @GetMapping("/{id}")
    public CardResponse getCard(@PathVariable UUID id) {
        return CardResponse.from(cardService.getCard(id));
    }

    @PatchMapping("/{id}/status")
    public CardResponse updateStatus(
            @PathVariable UUID id,
            @RequestParam Card.CardStatus status
    ) {
        return CardResponse.from(cardService.updateStatus(id, status));
    }

    @PutMapping("/{id}/limits")
    public CardResponse updateLimits(
            @PathVariable UUID id,
            @Valid @RequestBody List<SpendingLimitRequest> limits
    ) {
        cardService.updateLimits(id, limits);
        return CardResponse.from(cardService.getCard(id));
    }
}

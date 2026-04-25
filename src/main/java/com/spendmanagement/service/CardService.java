package com.spendmanagement.service;

import com.spendmanagement.api.dto.CardRequest;
import com.spendmanagement.api.dto.SpendingLimitRequest;
import com.spendmanagement.domain.Card;
import com.spendmanagement.domain.SpendingLimit;
import com.spendmanagement.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final SpendLimitService spendLimitService;

    @Transactional
    public Card createCard(CardRequest request) {
        Card card = Card.builder()
                .teamId(request.getTeamId())
                .holderId(request.getHolderId())
                .status(request.getStatus() != null ? request.getStatus() : Card.CardStatus.ACTIVE)
                .currency(request.getCurrency())
                .build();
        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public Card getCard(UUID cardId) {
        return cardRepository.findByIdWithLimits(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
    }

    @Transactional
    public Card updateStatus(UUID cardId, Card.CardStatus status) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        card.setStatus(status);
        return cardRepository.save(card);
    }

    @Transactional
    public List<SpendingLimit> updateLimits(UUID cardId, List<SpendingLimitRequest> limitRequests) {
        return spendLimitService.updateLimits(cardId, limitRequests);
    }
}

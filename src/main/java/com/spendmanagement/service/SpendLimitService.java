package com.spendmanagement.service;

import com.spendmanagement.api.dto.SpendingLimitRequest;
import com.spendmanagement.domain.Card;
import com.spendmanagement.domain.SpendingLimit;
import com.spendmanagement.domain.Transaction;
import com.spendmanagement.redis.SpendAggregationRepository;
import com.spendmanagement.repository.CardRepository;
import com.spendmanagement.repository.SpendingLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpendLimitService {

    private final SpendingLimitRepository spendingLimitRepository;
    private final SpendAggregationRepository spendAggregationRepository;
    private final CardRepository cardRepository;

    public Optional<String> checkLimits(Card card, BigDecimal amount) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID cardId = card.getId();

        for (SpendingLimit limit : card.getSpendingLimits()) {
            switch (limit.getLimitType()) {
                case PER_TRANSACTION -> {
                    if (amount.compareTo(limit.getAmount()) > 0) {
                        return Optional.of(Transaction.DeclineReason.LIMIT_EXCEEDED.name());
                    }
                }
                case DAILY -> {
                    BigDecimal current = spendAggregationRepository.getCurrentDailySpend(cardId, today);
                    if (current.add(amount).compareTo(limit.getAmount()) > 0) {
                        return Optional.of(Transaction.DeclineReason.LIMIT_EXCEEDED.name());
                    }
                }
                case WEEKLY -> {
                    BigDecimal current = spendAggregationRepository.getCurrentWeeklySpend(cardId, today);
                    if (current.add(amount).compareTo(limit.getAmount()) > 0) {
                        return Optional.of(Transaction.DeclineReason.LIMIT_EXCEEDED.name());
                    }
                }
                case MONTHLY -> {
                    BigDecimal current = spendAggregationRepository.getCurrentMonthlySpend(cardId, today);
                    if (current.add(amount).compareTo(limit.getAmount()) > 0) {
                        return Optional.of(Transaction.DeclineReason.LIMIT_EXCEEDED.name());
                    }
                }
            }
        }
        return Optional.empty();
    }

    public void recordSpend(UUID cardId, BigDecimal amount) {
        spendAggregationRepository.incrementSpend(cardId, amount, LocalDate.now(ZoneOffset.UTC));
    }

    public void reverseSpend(UUID cardId, BigDecimal amount) {
        spendAggregationRepository.decrementSpend(cardId, amount, LocalDate.now(ZoneOffset.UTC));
    }

    @Transactional
    public List<SpendingLimit> updateLimits(UUID cardId, List<SpendingLimitRequest> limitRequests) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        spendingLimitRepository.deleteByCardId(cardId);

        List<SpendingLimit> newLimits = limitRequests.stream()
                .map(req -> SpendingLimit.builder()
                        .card(card)
                        .limitType(req.getLimitType())
                        .amount(req.getAmount())
                        .currency(req.getCurrency())
                        .build())
                .toList();

        return spendingLimitRepository.saveAll(newLimits);
    }
}

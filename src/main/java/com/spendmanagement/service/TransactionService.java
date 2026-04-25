package com.spendmanagement.service;

import com.spendmanagement.api.dto.TransactionRequest;
import com.spendmanagement.api.dto.TransactionResponse;
import com.spendmanagement.domain.Card;
import com.spendmanagement.domain.Transaction;
import com.spendmanagement.kafka.TransactionEventProducer;
import com.spendmanagement.kafka.event.TransactionEvent;
import com.spendmanagement.repository.CardRepository;
import com.spendmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final SpendLimitService spendLimitService;
    private final PricingRuleService pricingRuleService;
    private final TransactionEventProducer eventProducer;
    private final WebhookService webhookService;

    @Transactional
    public TransactionResponse submit(TransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.debug("Idempotent request idempotencyKey={}", request.getIdempotencyKey());
            return TransactionResponse.from(existing.get());
        }

        Card card = cardRepository.findByIdWithLimits(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + request.getCardId()));

        Transaction transaction = Transaction.builder()
                .card(card)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .merchantCategory(request.getMerchantCategory())
                .idempotencyKey(request.getIdempotencyKey())
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        String declineReason = resolveDeclineReason(card, request.getAmount());

        if (declineReason != null) {
            transaction.setStatus(Transaction.TransactionStatus.DECLINED);
            transaction.setDeclineReason(declineReason);
            Transaction saved = transactionRepository.save(transaction);
            webhookService.notifyAsync(saved, card.getHolderId());
            return TransactionResponse.from(saved);
        }

        BigDecimal markup = pricingRuleService.calculateMarkup(
                request.getMerchantCategory(),
                request.getCurrency(),
                card.getId().toString()
        );
        transaction.setAppliedMarkup(markup.compareTo(BigDecimal.ZERO) == 0 ? null : markup);
        transaction.setStatus(Transaction.TransactionStatus.APPROVED);

        Transaction saved = transactionRepository.save(transaction);
        spendLimitService.recordSpend(card.getId(), request.getAmount());
        publishApprovedEvent(saved, card);
        webhookService.notifyAsync(saved, card.getHolderId());

        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse reverse(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != Transaction.TransactionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot reverse transaction in status: " + transaction.getStatus());
        }

        transaction.setStatus(Transaction.TransactionStatus.REVERSED);
        Transaction saved = transactionRepository.save(transaction);
        spendLimitService.reverseSpend(transaction.getCard().getId(), transaction.getAmount());

        TransactionEvent event = buildEvent(saved, saved.getCard());
        eventProducer.publish(event);

        return TransactionResponse.from(saved);
    }

    private String resolveDeclineReason(Card card, BigDecimal amount) {
        if (!card.isActive()) {
            return card.getStatus() == Card.CardStatus.BLOCKED
                    ? Transaction.DeclineReason.CARD_BLOCKED.name()
                    : Transaction.DeclineReason.CARD_CANCELLED.name();
        }
        return spendLimitService.checkLimits(card, amount).orElse(null);
    }

    private void publishApprovedEvent(Transaction transaction, Card card) {
        TransactionEvent event = buildEvent(transaction, card);
        eventProducer.publish(event);
    }

    private TransactionEvent buildEvent(Transaction transaction, Card card) {
        return TransactionEvent.builder()
                .transactionId(transaction.getId())
                .cardId(card.getId())
                .teamId(card.getTeamId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .merchantCategory(transaction.getMerchantCategory())
                .status(transaction.getStatus())
                .appliedMarkup(transaction.getAppliedMarkup())
                .occurredAt(Instant.now())
                .build();
    }
}

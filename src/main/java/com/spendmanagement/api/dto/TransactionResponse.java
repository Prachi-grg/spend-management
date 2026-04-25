package com.spendmanagement.api.dto;

import com.spendmanagement.domain.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {

    private UUID id;
    private UUID cardId;
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    private Transaction.TransactionStatus status;
    private String declineReason;
    private BigDecimal appliedMarkup;
    private String idempotencyKey;
    private Instant createdAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .cardId(t.getCard().getId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .merchantCategory(t.getMerchantCategory())
                .status(t.getStatus())
                .declineReason(t.getDeclineReason())
                .appliedMarkup(t.getAppliedMarkup())
                .idempotencyKey(t.getIdempotencyKey())
                .createdAt(t.getCreatedAt())
                .build();
    }
}

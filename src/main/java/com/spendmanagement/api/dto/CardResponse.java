package com.spendmanagement.api.dto;

import com.spendmanagement.domain.Card;
import com.spendmanagement.domain.SpendingLimit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CardResponse {

    private UUID id;
    private UUID teamId;
    private UUID holderId;
    private Card.CardStatus status;
    private String currency;
    private List<LimitDto> spendingLimits;
    private Instant createdAt;

    @Data
    @Builder
    public static class LimitDto {
        private SpendingLimit.LimitType limitType;
        private BigDecimal amount;
        private String currency;
    }

    public static CardResponse from(Card card) {
        List<LimitDto> limits = card.getSpendingLimits() == null ? List.of() :
                card.getSpendingLimits().stream()
                        .map(l -> LimitDto.builder()
                                .limitType(l.getLimitType())
                                .amount(l.getAmount())
                                .currency(l.getCurrency())
                                .build())
                        .toList();

        return CardResponse.builder()
                .id(card.getId())
                .teamId(card.getTeamId())
                .holderId(card.getHolderId())
                .status(card.getStatus())
                .currency(card.getCurrency())
                .spendingLimits(limits)
                .createdAt(card.getCreatedAt())
                .build();
    }
}

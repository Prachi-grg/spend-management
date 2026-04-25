package com.spendmanagement.kafka.event;

import com.spendmanagement.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private UUID transactionId;
    private UUID cardId;
    private UUID teamId;
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    private Transaction.TransactionStatus status;
    private BigDecimal appliedMarkup;
    private Instant occurredAt;
}

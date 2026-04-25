package com.spendmanagement.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_category", length = 64)
    private String merchantCategory;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "transaction_status", nullable = false)
    private TransactionStatus status;

    @Column(name = "decline_reason", length = 64)
    private String declineReason;

    @Column(name = "applied_markup", precision = 19, scale = 4)
    private BigDecimal appliedMarkup;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum TransactionStatus {
        PENDING, APPROVED, DECLINED, REVERSED
    }

    public enum DeclineReason {
        LIMIT_EXCEEDED, CARD_BLOCKED, CARD_CANCELLED, INSUFFICIENT_FUNDS
    }
}

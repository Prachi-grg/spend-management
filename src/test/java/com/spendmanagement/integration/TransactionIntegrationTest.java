package com.spendmanagement.integration;

import com.spendmanagement.api.dto.*;
import com.spendmanagement.domain.Card;
import com.spendmanagement.domain.SpendingLimit;
import com.spendmanagement.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID cardId;

    @BeforeEach
    void setUp() {
        CardRequest cardReq = new CardRequest();
        cardReq.setTeamId(UUID.randomUUID());
        cardReq.setHolderId(UUID.randomUUID());
        cardReq.setStatus(Card.CardStatus.ACTIVE);
        cardReq.setCurrency("EUR");

        ResponseEntity<CardResponse> cardResp = restTemplate.postForEntity(
                "/cards", cardReq, CardResponse.class);
        assertThat(cardResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        cardId = cardResp.getBody().getId();

        SpendingLimitRequest dailyLimit = new SpendingLimitRequest();
        dailyLimit.setLimitType(SpendingLimit.LimitType.DAILY);
        dailyLimit.setAmount(new BigDecimal("500.00"));
        dailyLimit.setCurrency("EUR");

        SpendingLimitRequest perTxLimit = new SpendingLimitRequest();
        perTxLimit.setLimitType(SpendingLimit.LimitType.PER_TRANSACTION);
        perTxLimit.setAmount(new BigDecimal("200.00"));
        perTxLimit.setCurrency("EUR");

        restTemplate.exchange(
                "/cards/" + cardId + "/limits",
                HttpMethod.PUT,
                new HttpEntity<>(List.of(dailyLimit, perTxLimit)),
                CardResponse.class
        );
    }

    @Test
    void transactionApprovedWithinLimit() {
        TransactionRequest req = buildRequest(new BigDecimal("100.00"), UUID.randomUUID().toString());

        ResponseEntity<TransactionResponse> resp = restTemplate.postForEntity(
                "/transactions", req, TransactionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getStatus()).isEqualTo(Transaction.TransactionStatus.APPROVED);
        assertThat(resp.getBody().getDeclineReason()).isNull();
        assertThat(resp.getBody().getId()).isNotNull();
    }

    @Test
    void transactionDeclinedWhenDailyLimitExceeded() {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        String key3 = UUID.randomUUID().toString();

        restTemplate.postForEntity("/transactions", buildRequest(new BigDecimal("150.00"), key1), TransactionResponse.class);
        restTemplate.postForEntity("/transactions", buildRequest(new BigDecimal("150.00"), key2), TransactionResponse.class);
        restTemplate.postForEntity("/transactions", buildRequest(new BigDecimal("150.00"), key3), TransactionResponse.class);

        ResponseEntity<TransactionResponse> resp = restTemplate.postForEntity(
                "/transactions",
                buildRequest(new BigDecimal("100.00"), UUID.randomUUID().toString()),
                TransactionResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getStatus()).isEqualTo(Transaction.TransactionStatus.DECLINED);
        assertThat(resp.getBody().getDeclineReason()).isEqualTo("LIMIT_EXCEEDED");
    }

    @Test
    void idempotentSubmissionReturnsSameResponse() {
        String idempotencyKey = UUID.randomUUID().toString();
        TransactionRequest req = buildRequest(new BigDecimal("50.00"), idempotencyKey);

        ResponseEntity<TransactionResponse> first = restTemplate.postForEntity(
                "/transactions", req, TransactionResponse.class);
        ResponseEntity<TransactionResponse> second = restTemplate.postForEntity(
                "/transactions", req, TransactionResponse.class);

        assertThat(first.getBody().getId()).isEqualTo(second.getBody().getId());
        assertThat(first.getBody().getStatus()).isEqualTo(second.getBody().getStatus());
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void pricingRuleAppliedToMatchingMerchantCategory() {
        FeatureFlagRequest flag = new FeatureFlagRequest();
        flag.setFlagKey("pricing-v2");
        flag.setEnabled(true);
        flag.setRolloutPercentage(100);
        restTemplate.postForEntity("/flags", flag, FeatureFlagResponse.class);

        PricingRuleRequest rule = new PricingRuleRequest();
        rule.setMerchantCategory("TRAVEL");
        rule.setMarkupPercentage(new BigDecimal("0.0150"));
        rule.setEffectiveFrom(java.time.Instant.now().minusSeconds(60));
        restTemplate.postForEntity("/rules", rule, PricingRuleResponse.class);

        TransactionRequest req = buildRequest(new BigDecimal("100.00"), UUID.randomUUID().toString());
        req.setMerchantCategory("TRAVEL");

        ResponseEntity<TransactionResponse> resp = restTemplate.postForEntity(
                "/transactions", req, TransactionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getStatus()).isEqualTo(Transaction.TransactionStatus.APPROVED);
        assertThat(resp.getBody().getAppliedMarkup())
                .isNotNull()
                .isEqualByComparingTo(new BigDecimal("0.0150"));
    }

    private TransactionRequest buildRequest(BigDecimal amount, String idempotencyKey) {
        TransactionRequest req = new TransactionRequest();
        req.setCardId(cardId);
        req.setAmount(amount);
        req.setCurrency("EUR");
        req.setMerchantCategory("RETAIL");
        req.setIdempotencyKey(idempotencyKey);
        return req;
    }
}

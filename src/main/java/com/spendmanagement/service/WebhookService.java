package com.spendmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendmanagement.domain.Transaction;
import com.spendmanagement.domain.WebhookDelivery;
import com.spendmanagement.repository.WebhookConfigurationRepository;
import com.spendmanagement.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookConfigurationRepository webhookConfigRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.max-retries:3}")
    private int maxRetries;

    @Value("${app.webhook.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAsync(Transaction transaction, UUID holderId) {
        webhookConfigRepository.findByHolderId(holderId)
                .filter(config -> config.isEnabled())
                .ifPresent(config -> deliver(transaction, config.getUrl()));
    }

    private void deliver(Transaction transaction, String url) {
        WebhookDelivery delivery = webhookDeliveryRepository.save(
                WebhookDelivery.builder()
                        .transaction(transaction)
                        .webhookUrl(url)
                        .eventType("transaction." + transaction.getStatus().name().toLowerCase())
                        .build()
        );

        String payload = buildPayload(transaction);
        int attempts = 0;

        while (attempts < maxRetries) {
            attempts++;
            delivery.setAttemptCount(attempts);
            delivery.setLastAttempted(Instant.now());

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("X-Event-Type", delivery.getEventType())
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                delivery.setResponseStatus(response.statusCode());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    delivery.setStatus(WebhookDelivery.WebhookStatus.DELIVERED);
                    webhookDeliveryRepository.save(delivery);
                    return;
                }

                log.warn("Webhook returned non-2xx status={} transactionId={} attempt={}",
                        response.statusCode(), transaction.getId(), attempts);
            } catch (Exception e) {
                log.warn("Webhook delivery failed transactionId={} attempt={}", transaction.getId(), attempts, e);
            }

            if (attempts < maxRetries) {
                sleep(initialBackoffMs * (1L << (attempts - 1)));
            }
        }

        delivery.setStatus(WebhookDelivery.WebhookStatus.FAILED);
        webhookDeliveryRepository.save(delivery);
        log.error("Webhook delivery exhausted retries transactionId={}", transaction.getId());
    }

    private String buildPayload(Transaction transaction) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "transactionId", transaction.getId().toString(),
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "status", transaction.getStatus().name(),
                    "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize webhook payload", e);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

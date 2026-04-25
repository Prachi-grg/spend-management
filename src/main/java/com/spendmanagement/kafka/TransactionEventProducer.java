package com.spendmanagement.kafka;

import com.spendmanagement.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${app.kafka.topics.transactions}")
    private String transactionsTopic;

    public void publish(TransactionEvent event) {
        String key = event.getCardId().toString();
        kafkaTemplate.send(transactionsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event transactionId={}", event.getTransactionId(), ex);
                    } else {
                        log.debug("Published transaction event transactionId={} partition={} offset={}",
                                event.getTransactionId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

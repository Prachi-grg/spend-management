package com.spendmanagement.kafka;

import com.spendmanagement.domain.Transaction;
import com.spendmanagement.kafka.event.TransactionEvent;
import com.spendmanagement.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingEventConsumer {

    private final BillingService billingService;

    @KafkaListener(
            topics = "${app.kafka.topics.transactions}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionEvent(
            TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack
    ) {
        log.debug("Received transaction event transactionId={} partition={} offset={}",
                event.getTransactionId(), partition, offset);

        if (event.getStatus() == Transaction.TransactionStatus.APPROVED) {
            try {
                billingService.recordTransaction(event);
                ack.acknowledge();
            } catch (Exception e) {
                log.error("Failed to process billing for transactionId={}", event.getTransactionId(), e);
                throw e;
            }
        } else {
            ack.acknowledge();
        }
    }
}

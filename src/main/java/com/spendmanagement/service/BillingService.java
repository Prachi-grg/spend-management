package com.spendmanagement.service;

import com.spendmanagement.api.dto.BillingRecordResponse;
import com.spendmanagement.domain.BillingLineItem;
import com.spendmanagement.domain.BillingRecord;
import com.spendmanagement.kafka.event.TransactionEvent;
import com.spendmanagement.repository.BillingLineItemRepository;
import com.spendmanagement.repository.BillingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final BillingRecordRepository billingRecordRepository;
    private final BillingLineItemRepository billingLineItemRepository;

    @Transactional
    public void recordTransaction(TransactionEvent event) {
        String period = toPeriod(event.getOccurredAt());
        UUID teamId = event.getTeamId();

        BillingRecord record = billingRecordRepository
                .findByTeamIdAndBillingPeriod(teamId, period)
                .orElseGet(() -> billingRecordRepository.save(
                        BillingRecord.builder()
                                .teamId(teamId)
                                .billingPeriod(period)
                                .currency(event.getCurrency())
                                .build()
                ));

        record.setTotalAmount(record.getTotalAmount().add(event.getAmount()));

        String category = event.getMerchantCategory() != null ? event.getMerchantCategory() : "UNCATEGORIZED";

        BillingLineItem lineItem = billingLineItemRepository
                .findByBillingRecordIdAndMerchantCategory(record.getId(), category)
                .orElseGet(() -> {
                    BillingLineItem newItem = BillingLineItem.builder()
                            .billingRecord(record)
                            .merchantCategory(category)
                            .build();
                    return billingLineItemRepository.save(newItem);
                });

        lineItem.setTransactionCount(lineItem.getTransactionCount() + 1);
        lineItem.setTotalAmount(lineItem.getTotalAmount().add(event.getAmount()));

        billingLineItemRepository.save(lineItem);
        billingRecordRepository.save(record);

        log.debug("Recorded billing transactionId={} teamId={} period={}", event.getTransactionId(), teamId, period);
    }

    @Transactional(readOnly = true)
    public BillingRecordResponse getBillingRecord(UUID teamId, String period) {
        BillingRecord record = billingRecordRepository
                .findByTeamIdAndBillingPeriod(teamId, period)
                .orElseGet(() -> BillingRecord.builder()
                        .teamId(teamId)
                        .billingPeriod(period)
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        return BillingRecordResponse.from(record);
    }

    private String toPeriod(Instant instant) {
        YearMonth ym = YearMonth.from(instant.atZone(ZoneOffset.UTC));
        return String.format("%d-%02d", ym.getYear(), ym.getMonthValue());
    }
}

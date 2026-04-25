package com.spendmanagement.api.dto;

import com.spendmanagement.domain.BillingLineItem;
import com.spendmanagement.domain.BillingRecord;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BillingRecordResponse {

    private UUID id;
    private UUID teamId;
    private String billingPeriod;
    private BigDecimal totalAmount;
    private String currency;
    private BillingRecord.BillingStatus status;
    private List<LineItemDto> lineItems;

    @Data
    @Builder
    public static class LineItemDto {
        private String merchantCategory;
        private int transactionCount;
        private BigDecimal totalAmount;
    }

    public static BillingRecordResponse from(BillingRecord record) {
        List<LineItemDto> items = record.getLineItems() == null ? List.of() :
                record.getLineItems().stream()
                        .map(BillingRecordResponse::toLineItemDto)
                        .toList();

        return BillingRecordResponse.builder()
                .id(record.getId())
                .teamId(record.getTeamId())
                .billingPeriod(record.getBillingPeriod())
                .totalAmount(record.getTotalAmount())
                .currency(record.getCurrency())
                .status(record.getStatus())
                .lineItems(items)
                .build();
    }

    private static LineItemDto toLineItemDto(BillingLineItem item) {
        return LineItemDto.builder()
                .merchantCategory(item.getMerchantCategory())
                .transactionCount(item.getTransactionCount())
                .totalAmount(item.getTotalAmount())
                .build();
    }
}

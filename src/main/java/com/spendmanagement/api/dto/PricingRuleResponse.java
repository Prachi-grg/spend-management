package com.spendmanagement.api.dto;

import com.spendmanagement.domain.PricingRule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PricingRuleResponse {

    private UUID id;
    private String merchantCategory;
    private String currencyCode;
    private BigDecimal markupPercentage;
    private Instant effectiveFrom;
    private Instant effectiveTo;

    public static PricingRuleResponse from(PricingRule rule) {
        return PricingRuleResponse.builder()
                .id(rule.getId())
                .merchantCategory(rule.getMerchantCategory())
                .currencyCode(rule.getCurrencyCode())
                .markupPercentage(rule.getMarkupPercentage())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .build();
    }
}

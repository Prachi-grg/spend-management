package com.spendmanagement.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PricingRuleRequest {

    @Size(max = 64)
    private String merchantCategory;

    @Size(min = 3, max = 3)
    private String currencyCode;

    @NotNull
    @DecimalMin("0.0001")
    @DecimalMax("1.0000")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal markupPercentage;

    @NotNull
    private Instant effectiveFrom;

    private Instant effectiveTo;
}

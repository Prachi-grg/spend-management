package com.spendmanagement.api.dto;

import com.spendmanagement.domain.SpendingLimit;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpendingLimitRequest {

    @NotNull(message = "limitType is required")
    private SpendingLimit.LimitType limitType;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3)
    private String currency;
}

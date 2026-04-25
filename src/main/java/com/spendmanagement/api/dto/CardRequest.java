package com.spendmanagement.api.dto;

import com.spendmanagement.domain.Card;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CardRequest {

    @NotNull
    private UUID teamId;

    @NotNull
    private UUID holderId;

    @NotNull
    private Card.CardStatus status;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;
}

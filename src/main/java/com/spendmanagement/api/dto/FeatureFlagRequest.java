package com.spendmanagement.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class FeatureFlagRequest {

    @NotBlank
    @Size(max = 128)
    private String flagKey;

    private boolean enabled;

    @Min(0)
    @Max(100)
    private int rolloutPercentage;

    private List<String> targetEntityIds;
}

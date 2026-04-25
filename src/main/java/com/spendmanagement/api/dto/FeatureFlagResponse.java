package com.spendmanagement.api.dto;

import com.spendmanagement.domain.FeatureFlag;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeatureFlagResponse {

    private UUID id;
    private String flagKey;
    private boolean enabled;
    private int rolloutPercentage;
    private List<String> targetEntityIds;

    public static FeatureFlagResponse from(FeatureFlag flag) {
        return FeatureFlagResponse.builder()
                .id(flag.getId())
                .flagKey(flag.getFlagKey())
                .enabled(flag.isEnabled())
                .rolloutPercentage(flag.getRolloutPercentage())
                .targetEntityIds(flag.getTargetEntityIds())
                .build();
    }
}

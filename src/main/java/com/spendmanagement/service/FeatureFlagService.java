package com.spendmanagement.service;

import com.spendmanagement.api.dto.FeatureFlagRequest;
import com.spendmanagement.domain.FeatureFlag;
import com.spendmanagement.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    public boolean isEnabled(String flagKey, String entityId) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .map(flag -> evaluate(flag, entityId))
                .orElse(false);
    }

    private boolean evaluate(FeatureFlag flag, String entityId) {
        if (!flag.isEnabled()) return false;

        if (flag.getTargetEntityIds() != null && flag.getTargetEntityIds().contains(entityId)) {
            return true;
        }

        if (flag.getRolloutPercentage() > 0) {
            int bucket = Math.abs((flag.getFlagKey() + entityId).hashCode()) % 100;
            return bucket < flag.getRolloutPercentage();
        }

        return false;
    }

    @Transactional
    public FeatureFlag upsert(FeatureFlagRequest request) {
        FeatureFlag flag = featureFlagRepository.findByFlagKey(request.getFlagKey())
                .orElseGet(() -> FeatureFlag.builder().flagKey(request.getFlagKey()).build());

        flag.setEnabled(request.isEnabled());
        flag.setRolloutPercentage(request.getRolloutPercentage());
        flag.setTargetEntityIds(request.getTargetEntityIds());

        return featureFlagRepository.save(flag);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> listAll() {
        return featureFlagRepository.findAll();
    }
}

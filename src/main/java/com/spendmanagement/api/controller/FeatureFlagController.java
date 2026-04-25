package com.spendmanagement.api.controller;

import com.spendmanagement.api.dto.FeatureFlagRequest;
import com.spendmanagement.api.dto.FeatureFlagResponse;
import com.spendmanagement.service.FeatureFlagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flags")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @PostMapping
    public FeatureFlagResponse upsert(@Valid @RequestBody FeatureFlagRequest request) {
        return FeatureFlagResponse.from(featureFlagService.upsert(request));
    }

    @GetMapping
    public List<FeatureFlagResponse> listAll() {
        return featureFlagService.listAll().stream()
                .map(FeatureFlagResponse::from)
                .toList();
    }

    @GetMapping("/{key}/evaluate")
    public Map<String, Object> evaluate(
            @PathVariable String key,
            @RequestParam String entityId
    ) {
        boolean enabled = featureFlagService.isEnabled(key, entityId);
        return Map.of("flagKey", key, "entityId", entityId, "enabled", enabled);
    }
}

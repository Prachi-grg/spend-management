package com.spendmanagement.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendmanagement.domain.PricingRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PricingRuleCache {

    private static final String CACHE_KEY = "pricing-rules:active";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.pricing-rules-ttl-minutes:10}")
    private int ttlMinutes;

    public Optional<List<PricingRule>> get() {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY);
            if (json == null) return Optional.empty();
            List<PricingRule> rules = objectMapper.readValue(json, new TypeReference<>() {});
            return Optional.of(rules);
        } catch (Exception e) {
            log.warn("Failed to read pricing rules from cache", e);
            return Optional.empty();
        }
    }

    public void put(List<PricingRule> rules) {
        try {
            String json = objectMapper.writeValueAsString(rules);
            redisTemplate.opsForValue().set(CACHE_KEY, json, Duration.ofMinutes(ttlMinutes));
        } catch (Exception e) {
            log.warn("Failed to write pricing rules to cache", e);
        }
    }

    public void evict() {
        redisTemplate.delete(CACHE_KEY);
    }
}

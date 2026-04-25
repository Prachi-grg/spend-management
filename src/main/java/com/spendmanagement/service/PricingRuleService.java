package com.spendmanagement.service;

import com.spendmanagement.api.dto.PricingRuleRequest;
import com.spendmanagement.domain.PricingRule;
import com.spendmanagement.redis.PricingRuleCache;
import com.spendmanagement.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final PricingRuleCache pricingRuleCache;
    private final FeatureFlagService featureFlagService;

    private static final String PRICING_V2_FLAG = "pricing-v2";

    public BigDecimal calculateMarkup(String merchantCategory, String currency, String cardId) {
        if (!featureFlagService.isEnabled(PRICING_V2_FLAG, cardId)) {
            return BigDecimal.ZERO;
        }

        List<PricingRule> rules = getActiveRules();
        Instant now = Instant.now();

        return rules.stream()
                .filter(rule -> rule.isActiveAt(now))
                .filter(rule -> matchesTransaction(rule, merchantCategory, currency))
                .map(PricingRule::getMarkupPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean matchesTransaction(PricingRule rule, String merchantCategory, String currency) {
        boolean categoryMatch = rule.getMerchantCategory() == null
                || rule.getMerchantCategory().equalsIgnoreCase(merchantCategory);
        boolean currencyMatch = rule.getCurrencyCode() == null
                || rule.getCurrencyCode().equalsIgnoreCase(currency);
        return categoryMatch && currencyMatch;
    }

    @Transactional
    public PricingRule createRule(PricingRuleRequest request) {
        PricingRule rule = PricingRule.builder()
                .merchantCategory(request.getMerchantCategory())
                .currencyCode(request.getCurrencyCode())
                .markupPercentage(request.getMarkupPercentage())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        PricingRule saved = pricingRuleRepository.save(rule);
        pricingRuleCache.evict();
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PricingRule> getActiveRules() {
        return pricingRuleCache.get().orElseGet(() -> {
            List<PricingRule> rules = pricingRuleRepository.findAllActive(Instant.now());
            pricingRuleCache.put(rules);
            return rules;
        });
    }
}

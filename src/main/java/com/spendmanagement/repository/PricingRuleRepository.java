package com.spendmanagement.repository;

import com.spendmanagement.domain.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

    @Query("""
            SELECT r FROM PricingRule r
            WHERE r.effectiveFrom <= :now
              AND (r.effectiveTo IS NULL OR r.effectiveTo > :now)
            ORDER BY r.effectiveFrom DESC
            """)
    List<PricingRule> findAllActive(Instant now);
}

package com.spendmanagement.repository;

import com.spendmanagement.domain.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, UUID> {

    @Query("""
            SELECT br FROM BillingRecord br
            LEFT JOIN FETCH br.lineItems
            WHERE br.teamId = :teamId AND br.billingPeriod = :period
            """)
    Optional<BillingRecord> findByTeamIdAndBillingPeriod(UUID teamId, String period);
}

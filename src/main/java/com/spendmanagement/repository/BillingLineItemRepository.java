package com.spendmanagement.repository;

import com.spendmanagement.domain.BillingLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingLineItemRepository extends JpaRepository<BillingLineItem, UUID> {

    Optional<BillingLineItem> findByBillingRecordIdAndMerchantCategory(UUID billingRecordId, String merchantCategory);
}

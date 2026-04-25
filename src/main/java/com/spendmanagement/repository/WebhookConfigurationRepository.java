package com.spendmanagement.repository;

import com.spendmanagement.domain.WebhookConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookConfigurationRepository extends JpaRepository<WebhookConfiguration, UUID> {

    Optional<WebhookConfiguration> findByHolderId(UUID holderId);
}

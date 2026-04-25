package com.spendmanagement.repository;

import com.spendmanagement.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    @Query("SELECT c FROM Card c LEFT JOIN FETCH c.spendingLimits WHERE c.id = :id")
    Optional<Card> findByIdWithLimits(UUID id);
}

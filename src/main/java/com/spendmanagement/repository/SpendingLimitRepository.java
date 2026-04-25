package com.spendmanagement.repository;

import com.spendmanagement.domain.SpendingLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, UUID> {

    List<SpendingLimit> findByCardId(UUID cardId);

    @Modifying
    @Query("DELETE FROM SpendingLimit sl WHERE sl.card.id = :cardId")
    void deleteByCardId(UUID cardId);
}

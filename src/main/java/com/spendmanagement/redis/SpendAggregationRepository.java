package com.spendmanagement.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SpendAggregationRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.redis.spend-key-ttl-days:35}")
    private int spendKeyTtlDays;

    private static final int SCALE = 4;

    public BigDecimal getCurrentDailySpend(UUID cardId, LocalDate date) {
        return getSpend(dailyKey(cardId, date));
    }

    public BigDecimal getCurrentWeeklySpend(UUID cardId, LocalDate date) {
        return getSpend(weeklyKey(cardId, date));
    }

    public BigDecimal getCurrentMonthlySpend(UUID cardId, LocalDate date) {
        return getSpend(monthlyKey(cardId, date));
    }

    public void incrementSpend(UUID cardId, BigDecimal amount, LocalDate date) {
        long amountInMicros = toMicros(amount);
        Duration ttl = Duration.ofDays(spendKeyTtlDays);

        incrementKey(dailyKey(cardId, date), amountInMicros, ttl);
        incrementKey(weeklyKey(cardId, date), amountInMicros, ttl);
        incrementKey(monthlyKey(cardId, date), amountInMicros, ttl);
    }

    public void decrementSpend(UUID cardId, BigDecimal amount, LocalDate date) {
        long amountInMicros = toMicros(amount);
        Duration ttl = Duration.ofDays(spendKeyTtlDays);

        decrementKey(dailyKey(cardId, date), amountInMicros, ttl);
        decrementKey(weeklyKey(cardId, date), amountInMicros, ttl);
        decrementKey(monthlyKey(cardId, date), amountInMicros, ttl);
    }

    private BigDecimal getSpend(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return BigDecimal.ZERO;
        return fromMicros(Long.parseLong(value));
    }

    private void incrementKey(String key, long amountInMicros, Duration ttl) {
        Boolean exists = redisTemplate.hasKey(key);
        redisTemplate.opsForValue().increment(key, amountInMicros);
        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.expire(key, ttl);
        }
    }

    private void decrementKey(String key, long amountInMicros, Duration ttl) {
        Boolean exists = redisTemplate.hasKey(key);
        redisTemplate.opsForValue().decrement(key, amountInMicros);
        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.expire(key, ttl);
        }
    }

    private String dailyKey(UUID cardId, LocalDate date) {
        return String.format("spend:%s:daily:%s", cardId, date);
    }

    private String weeklyKey(UUID cardId, LocalDate date) {
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = date.get(WeekFields.ISO.weekBasedYear());
        return String.format("spend:%s:weekly:%d-W%02d", cardId, year, week);
    }

    private String monthlyKey(UUID cardId, LocalDate date) {
        return String.format("spend:%s:monthly:%d-%02d", cardId, date.getYear(), date.getMonthValue());
    }

    private long toMicros(BigDecimal amount) {
        return amount.scaleByPowerOfTen(SCALE).longValue();
    }

    private BigDecimal fromMicros(long micros) {
        return BigDecimal.valueOf(micros, SCALE);
    }
}

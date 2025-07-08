package org.kifiya.paymentapi.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

    private final RedissonClient redissonClient;

    @Value("${payments.rate-limit}")
    private int rateLimit;

    private static final String RATE_LIMITER_KEY = "payments:rate_limiter";

    private RRateLimiter limiter;

    @PostConstruct
    private void initLimiter() {
        limiter = redissonClient.getRateLimiter(RATE_LIMITER_KEY);
        limiter.trySetRate(RateType.OVERALL, rateLimit, Duration.ofSeconds(1));
    }

    /**
     * Attempts to acquire one permit, waiting up to 500 ms.
     */
    public boolean tryAcquire() {
        return limiter.tryAcquire(1, Duration.ofMillis(500));
    }
}

package com.movieapp.util;

import com.movieapp.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimiter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    // Authenticated user limits
    private final int authenticatedCapacity;
    private final int authenticatedRefillRate;

    // Anonymous user limits (more restrictive)
    private final int anonymousCapacity;
    private final int anonymousRefillRate;

    public RateLimiter(
            @Value("${rate-limit.authenticated.capacity:100}") int authenticatedCapacity,
            @Value("${rate-limit.authenticated.refill-rate:10}") int authenticatedRefillRate,
            @Value("${rate-limit.anonymous.capacity:20}") int anonymousCapacity,
            @Value("${rate-limit.anonymous.refill-rate:2}") int anonymousRefillRate) {

        this.authenticatedCapacity = authenticatedCapacity;
        this.authenticatedRefillRate = authenticatedRefillRate;
        this.anonymousCapacity = anonymousCapacity;
        this.anonymousRefillRate = anonymousRefillRate;

        log.info("Rate Limiter initialized - Authenticated: {}/min, Anonymous: {}/min",
                authenticatedCapacity, anonymousCapacity);
    }

    public void checkRateLimit(String key) {
        Bucket bucket = cache.computeIfAbsent(key, k -> createBucket(k));

        long availableTokens = bucket.getAvailableTokens();

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for key: {} (available tokens: {})", key, availableTokens);
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please slow down and try again later."
            );
        }

        log.debug("Rate limit check passed for key: {} (remaining tokens: {})",
                key, bucket.getAvailableTokens());
    }

    /**
     * Create bucket based on key type (anonymous vs authenticated)
     */
    private Bucket createBucket(String key) {
        if (key.equals("anonymous")) {
            // More restrictive limits for anonymous users
            return createAnonymousBucket();
        } else {
            // Standard limits for authenticated users
            return createAuthenticatedBucket();
        }
    }

    /**
     * Create bucket for authenticated users
     * Default: 100 requests with 10 refill per second
     */
    private Bucket createAuthenticatedBucket() {
        Bandwidth limit = Bandwidth.classic(
                authenticatedCapacity,
                Refill.intervally(authenticatedRefillRate, Duration.ofSeconds(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for anonymous users
     * Default: 20 requests with 2 refill per second
     */
    private Bucket createAnonymousBucket() {
        Bandwidth limit = Bandwidth.classic(
                anonymousCapacity,
                Refill.intervally(anonymousRefillRate, Duration.ofSeconds(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Reset limit for specific key
     */
    public void resetLimit(String key) {
        cache.remove(key);
        log.info("Rate limit reset for key: {}", key);
    }

    /**
     * Get available tokens for a key (for monitoring)
     */
    public long getAvailableTokens(String key) {
        Bucket bucket = cache.get(key);
        return bucket != null ? bucket.getAvailableTokens() : 0;
    }

    /**
     * Clear all rate limit buckets (useful for testing)
     */
    public void clearAll() {
        cache.clear();
        log.info("All rate limit buckets cleared");
    }
}
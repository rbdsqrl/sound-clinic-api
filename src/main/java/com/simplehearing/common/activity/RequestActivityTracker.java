package com.simplehearing.common.activity;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory "when did a real request last come through" marker — lets {@code /health/db} skip
 * its own DB-keepalive query when genuine traffic already touched the database recently, instead
 * of always running it on every 5-minute pinger hit. See {@link ActivityTrackingFilter}, the only
 * writer, and {@code HealthController#healthDb}, the only reader. Single JVM instance, so this is
 * fine as a plain in-memory field — nothing here needs to survive a restart or be shared across
 * instances.
 */
@Component
public class RequestActivityTracker {

    private final AtomicReference<Instant> lastActivityAt = new AtomicReference<>(Instant.now());

    void recordActivity() {
        lastActivityAt.set(Instant.now());
    }

    public boolean hasRecentActivity(Duration within) {
        return Duration.between(lastActivityAt.get(), Instant.now()).compareTo(within) < 0;
    }
}

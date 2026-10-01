package com.simplehearing.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.simplehearing.analytics.dto.CaseTrendResponse;
import com.simplehearing.analytics.dto.EngagementOverviewResponse;
import com.simplehearing.analytics.dto.ScheduleResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Short-lived in-process cache for the org-level analytics endpoints, so repeated loads of the
 * same tab (and several users on the same org) don't each recompute from the database.
 *
 * <p>Built for a 512MB instance, so it can't grow unbounded:
 * <ul>
 *   <li><b>Hard memory cap</b> — each cache is bounded by an estimated <i>weight in bytes</i>
 *       ({@link #MAX_BYTES_PER_CACHE}), not an entry count, because one entry's size varies from
 *       ~1KB (members table) to megabytes (a two-year daily trend for every case). An entry that
 *       would exceed the cap by itself is simply not retained.</li>
 *   <li><b>Short TTL</b> — entries expire {@link #TTL} after being written, so the data is never
 *       more than a minute stale and idle entries free themselves.</li>
 *   <li><b>Soft values</b> — if the JVM ever runs short of heap, the GC reclaims cached entries
 *       before it throws an OutOfMemoryError. The cache gives way to the app, never the reverse.</li>
 * </ul>
 * Only finished response DTOs are cached (immutable records), never entities or raw rows, and every
 * key starts with the org id so one organisation can never be served another's numbers.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ANALYTICS_MEMBERS    = "analyticsMembers";
    public static final String ANALYTICS_CASES      = "analyticsCases";
    public static final String ANALYTICS_TRENDS     = "analyticsCaseTrends";
    public static final String ANALYTICS_SNAPSHOT   = "analyticsSnapshot";
    public static final String ANALYTICS_ENGAGEMENT = "analyticsEngagement";
    public static final String ANALYTICS_HEATMAP    = "analyticsHeatmap";
    public static final String ANALYTICS_SCHEDULE   = "analyticsSchedule";

    static final Duration TTL = Duration.ofSeconds(60);

    /** Per cache — 7 caches, so at most ~21MB of estimated weight in the absolute worst case,
     *  and a few hundred KB in ordinary use. */
    static final long MAX_BYTES_PER_CACHE = 3L * 1024 * 1024;

    // Measured heap cost of a fully populated trend Bucket is ~240 bytes; weighed at 400 for headroom.
    private static final int BYTES_PER_BUCKET = 400;
    private static final int BYTES_PER_ROW = 400;
    private static final int BASE_BYTES = 1024;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                build(ANALYTICS_MEMBERS), build(ANALYTICS_CASES), build(ANALYTICS_TRENDS),
                build(ANALYTICS_SNAPSHOT), build(ANALYTICS_ENGAGEMENT), build(ANALYTICS_HEATMAP),
                build(ANALYTICS_SCHEDULE)));
        return manager;
    }

    private static CaffeineCache build(String name) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumWeight(MAX_BYTES_PER_CACHE)
                .weigher((Object key, Object value) -> weigh(value))
                .expireAfterWrite(TTL)
                .softValues()
                .build());
    }

    /** Deliberately generous estimate of an entry's heap footprint in bytes. */
    static int weigh(Object value) {
        long bytes = BASE_BYTES;
        if (value instanceof ScheduleResponse s) {
            bytes += (long) BYTES_PER_ROW * s.sessions().size();
        } else if (value instanceof EngagementOverviewResponse e) {
            // Each sessionsTrend point carries a small status->count map (~500 bytes).
            bytes += 700L * e.sessionsTrend().size() + BYTES_PER_ROW * (long) e.checklistFilledTrend().size();
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                bytes += item instanceof CaseTrendResponse t
                        ? 128L + (long) BYTES_PER_BUCKET * t.buckets().size()
                        : BYTES_PER_ROW;
            }
        } else {
            bytes += 4096;
        }
        return (int) Math.min(Integer.MAX_VALUE, bytes);
    }
}

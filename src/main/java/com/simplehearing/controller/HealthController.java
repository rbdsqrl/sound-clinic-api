package com.simplehearing.controller;

import com.simplehearing.common.activity.RequestActivityTracker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    /** Below this, real traffic already touched the DB recently enough that the keepalive
     *  ping's own query would just be redundant — see healthDb(). Matches the pinger's own
     *  5-minute interval (worker/keepalive.js). */
    private static final Duration SKIP_PING_WITHIN = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;
    private final RequestActivityTracker activityTracker;

    public HealthController(JdbcTemplate jdbcTemplate, RequestActivityTracker activityTracker) {
        this.jdbcTemplate = jdbcTemplate;
        this.activityTracker = activityTracker;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
            "service", "Simple Hearing & Speech Care API",
            "status",  "running",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "service",   "simple-hearing-api",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Runs a trivial query against the DB so an external uptime pinger (or a Render cron job)
     * can hit this on a short interval to stop the database's free-tier compute from
     * auto-suspending on idle, without needing auth. Not for correctness monitoring — just a
     * keepalive. Skips the actual query (still returns 200) when a real request already touched
     * the DB more recently than SKIP_PING_WITHIN — genuine traffic already reset Neon's own idle
     * clock, so running SELECT 1 on top of it is a wasted connection/query, not an additional
     * safeguard.
     */
    @GetMapping("/health/db")
    public ResponseEntity<Map<String, String>> healthDb() {
        if (activityTracker.hasRecentActivity(SKIP_PING_WITHIN)) {
            return ResponseEntity.ok(Map.of(
                "status",    "UP",
                "component", "database",
                "note",      "skipped — recent request activity already keeps the DB warm",
                "timestamp", Instant.now().toString()
            ));
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of(
                "status",    "UP",
                "component", "database",
                "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status",    "DOWN",
                "component", "database",
                "timestamp", Instant.now().toString()
            ));
        }
    }
}

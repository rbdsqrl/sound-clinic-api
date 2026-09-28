package com.simplehearing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
     * auto-suspending on idle, without needing auth. Not for correctness monitoring — just a keepalive.
     */
    @GetMapping("/health/db")
    public ResponseEntity<Map<String, String>> healthDb() {
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

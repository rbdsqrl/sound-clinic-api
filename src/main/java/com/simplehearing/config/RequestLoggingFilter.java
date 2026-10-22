package com.simplehearing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logs every request's method, path and final response status (plus duration) at INFO level.
 * Runs ahead of Spring Security (HIGHEST_PRECEDENCE) so it also sees requests that never make it
 * past auth, and it's a plain servlet Filter bean rather than one wired into SecurityConfig's
 * chain, so it isn't tied to whether a path is public or protected.
 *
 * Exists so calls that don't otherwise touch application code — like an external uptime pinger
 * hitting /health/db — are still visible in Render's logs, not just "nothing happened".
 *
 * Also logs a start line (not for CORS preflights) and the JVM's used heap and the number of
 * requests in flight on both lines. The finish line only appears once a request completes, so
 * a request that was running when the instance ran out of memory would otherwise leave no trace;
 * the last start line without a matching finish line, and the heap trend, point at it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final AtomicInteger inFlight = new AtomicInteger();

    private static long usedHeapMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String query = request.getQueryString();
        String path = query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();
        boolean preflight = "OPTIONS".equals(request.getMethod());
        int concurrent = inFlight.incrementAndGet();
        if (!preflight) {
            log.info("START {} {} [inflight={}, heap={}MB]", request.getMethod(), path, concurrent, usedHeapMb());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int remaining = inFlight.decrementAndGet();
            log.info("{} {} -> {} ({}ms) [inflight={}, heap={}MB]",
                    request.getMethod(), path, response.getStatus(), durationMs, remaining, usedHeapMb());
        }
    }
}

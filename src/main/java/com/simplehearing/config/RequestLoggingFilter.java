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

/**
 * Logs every request's method, path and final response status (plus duration) at INFO level.
 * Runs ahead of Spring Security (HIGHEST_PRECEDENCE) so it also sees requests that never make it
 * past auth, and it's a plain servlet Filter bean rather than one wired into SecurityConfig's
 * chain, so it isn't tied to whether a path is public or protected.
 *
 * Exists so calls that don't otherwise touch application code — like an external uptime pinger
 * hitting /health/db — are still visible in Render's logs, not just "nothing happened".
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String query = request.getQueryString();
            String path = query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();
            log.info("{} {} -> {} ({}ms)", request.getMethod(), path, response.getStatus(), durationMs);
        }
    }
}

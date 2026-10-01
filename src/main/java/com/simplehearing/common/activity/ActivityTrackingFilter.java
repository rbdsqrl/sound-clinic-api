package com.simplehearing.common.activity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Marks every request as activity — except {@code /health/db} itself, the keepalive pinger's own
 * hit, which would otherwise always look like "recent activity" and defeat the point of the skip
 * it enables (see {@link RequestActivityTracker}).
 */
@Component
public class ActivityTrackingFilter extends OncePerRequestFilter {

    private final RequestActivityTracker tracker;

    public ActivityTrackingFilter(RequestActivityTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!"/health/db".equals(request.getRequestURI())) {
            tracker.recordActivity();
        }
        filterChain.doFilter(request, response);
    }
}

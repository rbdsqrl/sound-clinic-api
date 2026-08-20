package com.simplehearing.analytics.controller;

import com.simplehearing.analytics.dto.CaseloadResponse;
import com.simplehearing.analytics.dto.TimeSeriesResponse;
import com.simplehearing.analytics.enums.Granularity;
import com.simplehearing.analytics.service.AnalyticsService;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Progress analytics over the inputs therapists already record against their patients.
 *
 * <p>All three endpoints are restricted to BUSINESS_OWNER, ADMIN and OFFICE_ADMIN. Therapists
 * and parents do not reach these routes, so the per-caller patient scoping used elsewhere in
 * the codebase is not applied here — org membership is the whole authorization rule, and every
 * query filters on {@code principal.getOrgId()}.
 */
@Tag(name = "Analytics", description = "Daily, weekly and monthly progress series")
@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Progress series for one patient, with per-domain breakdown")
    @GetMapping("/patients/{patientId}/progress")
    public ResponseEntity<ApiResponse<TimeSeriesResponse>> patientProgress(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "WEEKLY") Granularity granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String domain,
            @AuthenticationPrincipal UserPrincipal principal) {

        TimeSeriesResponse data = analyticsService.patientProgress(
                orgId(principal), patientId, granularity, from, to, domain);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "A therapist's caseload series plus a row per patient")
    @GetMapping("/therapists/{therapistId}/caseload")
    public ResponseEntity<ApiResponse<CaseloadResponse>> therapistCaseload(
            @PathVariable UUID therapistId,
            @RequestParam(defaultValue = "WEEKLY") Granularity granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String domain,
            @AuthenticationPrincipal UserPrincipal principal) {

        CaseloadResponse data = analyticsService.therapistCaseload(
                orgId(principal), therapistId, granularity, from, to, domain);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Organisation-wide rollup — weekly or monthly only")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<TimeSeriesResponse>> overview(
            @RequestParam(defaultValue = "MONTHLY") Granularity granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String domain,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Day-to-day movement across a whole org is churn, not signal — a daily org series would
        // read as noise and invite conclusions the data cannot support.
        if (granularity == Granularity.DAILY) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Daily granularity is only available per patient — use WEEKLY or MONTHLY here");
        }

        TimeSeriesResponse data = analyticsService.orgOverview(orgId(principal), granularity, from, to, domain);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private static UUID orgId(UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        if (orgId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No organisation on the current account");
        }
        return orgId;
    }
}

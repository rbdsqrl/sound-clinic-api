package com.simplehearing.analytics.controller;

import com.simplehearing.analytics.dto.ActivityProgressResponse;
import com.simplehearing.analytics.dto.CaseSummaryResponse;
import com.simplehearing.analytics.dto.CaseloadResponse;
import com.simplehearing.analytics.dto.EngagementOverviewResponse;
import com.simplehearing.analytics.dto.FrequencyResponse;
import com.simplehearing.analytics.dto.MemberSummaryResponse;
import com.simplehearing.analytics.dto.OrgSnapshotResponse;
import com.simplehearing.analytics.dto.ScheduleResponse;
import com.simplehearing.analytics.dto.TimeSeriesResponse;
import com.simplehearing.analytics.enums.Granularity;
import com.simplehearing.analytics.service.AnalyticsService;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.successcriteria.dto.SuccessCriteriaResponse;
import com.simplehearing.successcriteria.service.SuccessCriteriaService;
import com.simplehearing.user.enums.Role;
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
import java.util.List;
import java.util.UUID;

/**
 * Progress analytics over the inputs therapists already record against their patients.
 *
 * <p>The therapist-caseload and org-overview endpoints are restricted to BUSINESS_OWNER and
 * CLINIC_HEAD — org membership is the whole authorization rule there, and every query
 * filters on {@code principal.getOrgId()}. The per-patient progress and activity endpoints are
 * also open to PARENT, but only for a patient they're actually linked to — checked explicitly
 * in each method since the class-level guard can't express "own children only".
 */
@Tag(name = "Analytics", description = "Daily, weekly and monthly progress series")
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final PatientParentRepository patientParentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SuccessCriteriaService successCriteriaService;

    public AnalyticsController(
            AnalyticsService analyticsService,
            PatientParentRepository patientParentRepository,
            EnrollmentRepository enrollmentRepository,
            SuccessCriteriaService successCriteriaService) {
        this.analyticsService = analyticsService;
        this.patientParentRepository = patientParentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.successCriteriaService = successCriteriaService;
    }

    @Operation(summary = "Progress series for one patient, with per-domain breakdown")
    @GetMapping("/patients/{patientId}/progress")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT')")
    public ResponseEntity<ApiResponse<TimeSeriesResponse>> patientProgress(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "WEEKLY") Granularity granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String domain,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireViewable(patientId, principal);
        TimeSeriesResponse data = analyticsService.patientProgress(
                orgId(principal), patientId, granularity, from, to, domain);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Activity assignment/attempt progress for one patient — additive to /progress")
    @GetMapping("/patients/{patientId}/activities")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT')")
    public ResponseEntity<ApiResponse<ActivityProgressResponse>> patientActivityProgress(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireViewable(patientId, principal);
        ActivityProgressResponse data = analyticsService.patientActivityProgress(orgId(principal), patientId, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Session cadence for one patient, folded across every concurrent enrollment")
    @GetMapping("/patients/{patientId}/frequency")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT')")
    public ResponseEntity<ApiResponse<FrequencyResponse>> patientFrequency(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireViewable(patientId, principal);
        FrequencyResponse data = analyticsService.patientSessionFrequency(orgId(principal), patientId, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "A therapist's caseload series plus a row per patient")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
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

    @Operation(summary = "Org-wide clinical-outcome rollup — avg therapy duration, program breakdown, admission→discharge funnel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<OrgSnapshotResponse>> snapshot(@AuthenticationPrincipal UserPrincipal principal) {
        OrgSnapshotResponse data = analyticsService.orgSnapshot(orgId(principal));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Org-wide engagement rollup for the Overview analytics tab — users, sessions, skills, checklist fills")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    @GetMapping("/engagement-overview")
    public ResponseEntity<ApiResponse<EngagementOverviewResponse>> engagementOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        EngagementOverviewResponse data = analyticsService.engagementOverview(orgId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Session count per day in the window — powers the calendar heatmap")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    @GetMapping("/session-heatmap")
    public ResponseEntity<ApiResponse<List<EngagementOverviewResponse.TrendPoint>>> sessionHeatmap(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<EngagementOverviewResponse.TrendPoint> data = analyticsService.sessionHeatmap(orgId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "One row per active patient — sessions, members/activities assigned, checklist fills, LT goals, payment status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<CaseSummaryResponse>>> cases(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<CaseSummaryResponse> data = analyticsService.cases(orgId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "One row per therapist/doctor — cases/activities assigned, activities created, sessions cancelled, IEP plans")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MemberSummaryResponse>>> members(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<MemberSummaryResponse> data = analyticsService.members(orgId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Flat session log + KPI strip for the Schedule tab, optionally filtered by case/member/program")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<ScheduleResponse>> schedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID therapistId,
            @RequestParam(required = false) UUID programId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ScheduleResponse data = analyticsService.schedule(orgId(principal), from, to, patientId, therapistId, programId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Discharge success-criteria composite for one enrollment")
    @GetMapping("/enrollments/{enrollmentId}/success-criteria")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<SuccessCriteriaResponse>> successCriteria(
            @PathVariable UUID enrollmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getOrgId().equals(orgId(principal))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        requireViewable(enrollment.getPatientId(), principal);

        SuccessCriteriaResponse data = successCriteriaService.compute(orgId(principal), enrollmentId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** A parent may only pull analytics for a patient they're actually linked to. */
    private void requireViewable(UUID patientId, UserPrincipal principal) {
        if (!principal.getUser().hasRole(Role.PARENT)) {
            return;
        }
        boolean linked = patientParentRepository.findById_PatientId(patientId).stream()
                .anyMatch(pp -> pp.getId().getParentId().equals(principal.getId()));
        if (!linked) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
        }
    }

    private static UUID orgId(UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        if (orgId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No organisation on the current account");
        }
        return orgId;
    }
}

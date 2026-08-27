package com.simplehearing.baseline.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.baseline.dto.AddBaselineProgressRequest;
import com.simplehearing.baseline.dto.BaselineProgressEntryResponse;
import com.simplehearing.baseline.dto.BaselineReportResponse;
import com.simplehearing.baseline.dto.CreateBaselineReportRequest;
import com.simplehearing.baseline.dto.UpdateBaselineReportRequest;
import com.simplehearing.baseline.enums.BaselineDomain;
import com.simplehearing.baseline.service.BaselineReportService;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Baseline Report", description = "Baseline vs. current tracking across developmental domains")
@RestController
@RequestMapping("/api/v1/patients/{patientId}/baseline-report")
public class BaselineReportController {

    private final BaselineReportService service;
    private final PatientParentRepository patientParentRepository;

    public BaselineReportController(BaselineReportService service, PatientParentRepository patientParentRepository) {
        this.service = service;
        this.patientParentRepository = patientParentRepository;
    }

    @Operation(summary = "Get the patient's baseline report, or null if none has been created yet")
    @GetMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT')")
    public ResponseEntity<ApiResponse<BaselineReportResponse>> getReport(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireViewable(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(service.getReport(patientId, principal)));
    }

    @Operation(summary = "Create the baseline report for a patient — header fields and initial per-domain baseline values")
    @PostMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<BaselineReportResponse>> createReport(
            @PathVariable UUID patientId,
            @RequestBody CreateBaselineReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        BaselineReportResponse response = service.createReport(patientId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "Update the baseline report's header fields and/or per-domain baseline text")
    @PatchMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<BaselineReportResponse>> updateReport(
            @PathVariable UUID patientId,
            @RequestBody UpdateBaselineReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        BaselineReportResponse response = service.updateReport(patientId, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Log a dated 'current' entry for one domain")
    @PostMapping("/domains/{domain}/progress")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<BaselineProgressEntryResponse>> addProgress(
            @PathVariable UUID patientId,
            @PathVariable BaselineDomain domain,
            @Valid @RequestBody AddBaselineProgressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        BaselineProgressEntryResponse response = service.addProgress(patientId, domain, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "List one domain's dated 'current' entries, newest first")
    @GetMapping("/domains/{domain}/progress")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT')")
    public ResponseEntity<ApiResponse<List<BaselineProgressEntryResponse>>> listProgress(
            @PathVariable UUID patientId,
            @PathVariable BaselineDomain domain,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireViewable(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(service.listProgress(patientId, domain, principal)));
    }

    /** A parent may only view a baseline report for a patient they're actually linked to. */
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
}

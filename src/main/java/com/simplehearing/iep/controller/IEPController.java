package com.simplehearing.iep.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.iep.dto.*;
import com.simplehearing.iep.service.IEPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Tag(name = "IEP", description = "IEP plan and goal tracking")
@RestController
@RequestMapping("/api/v1/iep")
public class IEPController {

    private final IEPService iepService;

    public IEPController(IEPService iepService) {
        this.iepService = iepService;
    }

    // ── List plans for a patient ──────────────────────────────────────────────

    @Operation(summary = "List IEP plans — for a patient (patientId required) or all plans in org (admin only)")
    @GetMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN', 'PARENT')")
    public ResponseEntity<ApiResponse<List<IEPPlanResponse>>> listPlans(
            @RequestParam(required = false) UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (patientId != null) {
            return ResponseEntity.ok(ApiResponse.success(iepService.listPlans(patientId, principal)));
        }
        // Org-level listing restricted to admin roles
        String role = principal.getUser().getRole().name();
        if (!role.equals("ADMIN") && !role.equals("BUSINESS_OWNER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "patientId is required for your role");
        }
        return ResponseEntity.ok(ApiResponse.success(iepService.listAllPlans(principal)));
    }

    // ── Create a plan ─────────────────────────────────────────────────────────

    @Operation(summary = "Create an IEP plan for a patient")
    @PostMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<IEPPlanResponse>> createPlan(
            @RequestParam UUID patientId,
            @Valid @RequestBody CreateIEPPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        IEPPlanResponse response = iepService.createPlan(patientId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── Import plans + goals from CSV ─────────────────────────────────────────

    @Operation(summary = "Import IEP plans and goals from a CSV file")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportResultResponse>> importCsv(
            @RequestParam UUID patientId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        String csvContent;
        try {
            csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file: " + e.getMessage());
        }

        ImportResultResponse result = iepService.importCsv(patientId, csvContent, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    // ── Download sample CSV ───────────────────────────────────────────────────

    @Operation(summary = "Download sample IEP import CSV")
    @GetMapping("/sample-csv")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN', 'PARENT')")
    public ResponseEntity<String> sampleCsv(@AuthenticationPrincipal UserPrincipal principal) {
        String csv = iepService.sampleCsv();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"iep-import-sample.csv\"")
                .body(csv);
    }

    // ── Update a plan ─────────────────────────────────────────────────────────

    @Operation(summary = "Update an IEP plan")
    @PatchMapping("/{planId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<IEPPlanResponse>> updatePlan(
            @PathVariable UUID planId,
            @RequestBody UpdateIEPPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        IEPPlanResponse response = iepService.updatePlan(planId, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Delete a plan ─────────────────────────────────────────────────────────

    @Operation(summary = "Delete an IEP plan")
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<Void> deletePlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {

        iepService.deletePlan(planId, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Add a goal to a plan ──────────────────────────────────────────────────

    @Operation(summary = "Add a goal to an existing IEP plan")
    @PostMapping("/{planId}/goals")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<IEPGoalResponse>> addGoal(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateIEPGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        IEPGoalResponse response = iepService.addGoal(planId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── Update a goal ─────────────────────────────────────────────────────────

    @Operation(summary = "Update an IEP goal")
    @PatchMapping("/goals/{goalId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<IEPGoalResponse>> updateGoal(
            @PathVariable UUID goalId,
            @RequestBody UpdateIEPGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        IEPGoalResponse response = iepService.updateGoal(goalId, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Delete a goal ─────────────────────────────────────────────────────────

    @Operation(summary = "Delete an IEP goal")
    @DeleteMapping("/goals/{goalId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID goalId,
            @AuthenticationPrincipal UserPrincipal principal) {

        iepService.deleteGoal(goalId, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Add progress to a goal ────────────────────────────────────────────────

    @Operation(summary = "Record a progress entry for an IEP goal")
    @PostMapping("/goals/{goalId}/progress")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<IEPGoalResponse>> addProgress(
            @PathVariable UUID goalId,
            @Valid @RequestBody AddProgressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        IEPGoalResponse response = iepService.addProgress(goalId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}

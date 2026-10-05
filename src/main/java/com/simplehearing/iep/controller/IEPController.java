package com.simplehearing.iep.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.iep.dto.GoalPacingResponse;
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
    private final com.simplehearing.iep.service.GoalPacingService goalPacingService;
    private final com.simplehearing.iep.service.IEPCustomDomainService customDomainService;

    public IEPController(IEPService iepService, com.simplehearing.iep.service.GoalPacingService goalPacingService,
                         com.simplehearing.iep.service.IEPCustomDomainService customDomainService) {
        this.customDomainService = customDomainService;
        this.iepService = iepService;
        this.goalPacingService = goalPacingService;
    }

    // ── List plans for a patient ──────────────────────────────────────────────

    @Operation(summary = "List IEP plans — for a patient (patientId required) or all plans in org (admin only)")
    @GetMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<IEPPlanResponse>>> listPlans(
            @RequestParam(required = false) UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (patientId != null) {
            return ResponseEntity.ok(ApiResponse.success(iepService.listPlans(patientId, principal)));
        }
        // Org-level listing restricted to admin roles
        String role = principal.getUser().getRole().name();
        if (!role.equals("CLINIC_HEAD") && !role.equals("BUSINESS_OWNER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "patientId is required for your role");
        }
        return ResponseEntity.ok(ApiResponse.success(iepService.listAllPlans(principal)));
    }

    // ── Create a plan ─────────────────────────────────────────────────────────

    @Operation(summary = "Create an IEP plan for a patient")
    @PostMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT', 'OFFICE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<Void> deletePlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {

        iepService.deletePlan(planId, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Organisation's custom goal domains ────────────────────────────────────

    public record CustomDomainResponse(UUID id, String name) {}
    public record CreateCustomDomainRequest(@jakarta.validation.constraints.NotBlank String name) {}

    @Operation(summary = "The organisation's own IEP goal domains (in addition to the built-in ones)")
    @GetMapping("/custom-domains")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomDomainResponse>>> customDomains(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(customDomainService.list(principal.getOrgId()).stream()
                .map(d -> new CustomDomainResponse(d.getId(), d.getName())).toList()));
    }

    @Operation(summary = "Add a custom IEP goal domain (returns the existing one if the name is already there)")
    @PostMapping("/custom-domains")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<CustomDomainResponse>> createCustomDomain(
            @Valid @RequestBody CreateCustomDomainRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var d = customDomainService.findOrCreate(principal.getOrgId(), request.name(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new CustomDomainResponse(d.getId(), d.getName())));
    }

    @Operation(summary = "Remove a custom domain from the picker — goals already using it keep its name")
    @DeleteMapping("/custom-domains/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<Void> deleteCustomDomain(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        customDomainService.remove(principal.getOrgId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Goal pacing across the linked therapy's sessions ──────────────────────

    @Operation(summary = "How the plan's active goals are spread across the upcoming sessions of its linked therapy")
    @GetMapping("/{planId}/pacing")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<GoalPacingResponse>> pacing(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(goalPacingService.pacing(planId, principal.getOrgId())));
    }

    @Operation(summary = "Re-spread the plan's active goals across the upcoming sessions and set their target dates to match")
    @PostMapping("/{planId}/pacing/apply")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<GoalPacingResponse>> applyPacing(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(goalPacingService.apply(planId, principal.getOrgId())));
    }

    // ── Add a goal to a plan ──────────────────────────────────────────────────

    @Operation(summary = "Add a goal to an existing IEP plan")
    @PostMapping("/{planId}/goals")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID goalId,
            @AuthenticationPrincipal UserPrincipal principal) {

        iepService.deleteGoal(goalId, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Add progress to a goal ────────────────────────────────────────────────

    @Operation(summary = "Record a progress entry for an IEP goal")
    @PostMapping("/goals/{goalId}/progress")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<IEPGoalResponse>> addProgress(
            @PathVariable UUID goalId,
            @Valid @RequestBody AddProgressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        IEPGoalResponse response = iepService.addProgress(goalId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── List progress entries for a goal ──────────────────────────────────────

    @Operation(summary = "List progress entries for an IEP goal, newest first")
    @GetMapping("/goals/{goalId}/progress")
    @PreAuthorize("hasAnyRole('THERAPIST', 'BUSINESS_OWNER', 'CLINIC_HEAD', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<IEPGoalProgressResponse>>> listProgress(
            @PathVariable UUID goalId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<IEPGoalProgressResponse> response = iepService.listProgress(goalId, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.simplehearing.activity.controller;

import com.simplehearing.activity.dto.*;
import com.simplehearing.activity.entity.Activity;
import com.simplehearing.activity.entity.ActivityResource;
import com.simplehearing.activity.repository.ActivityResourceRepository;
import com.simplehearing.activity.service.ActivityService;
import com.simplehearing.activity.service.ActivitySharingService;
import com.simplehearing.activity.service.MagicFillService;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Activities", description = "Create, assign, and track therapy activities")
@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private static final String STAFF_ROLES = "'BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST'";

    private final ActivityService activityService;
    private final MagicFillService magicFillService;
    private final ActivitySharingService sharingService;
    private final ActivityResourceRepository resourceRepository;
    private final StorageService storageService;
    private final PatientParentRepository patientParentRepository;

    public ActivityController(ActivityService activityService, MagicFillService magicFillService,
                               ActivitySharingService sharingService, ActivityResourceRepository resourceRepository,
                               StorageService storageService, PatientParentRepository patientParentRepository) {
        this.activityService = activityService;
        this.magicFillService = magicFillService;
        this.sharingService = sharingService;
        this.resourceRepository = resourceRepository;
        this.storageService = storageService;
        this.patientParentRepository = patientParentRepository;
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    @Operation(summary = "List activities for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> list(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(activityService.list(principal.getOrgId(), activeOnly)));
    }

    @Operation(summary = "Create an activity")
    @PostMapping
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ActivityResponse>> create(
            @Valid @RequestBody CreateActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityResponse created = activityService.create(principal.getOrgId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Get one activity")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ActivityResponse>> get(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(activityService.get(id, principal.getOrgId())));
    }

    @Operation(summary = "Update an activity")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ActivityResponse>> update(
            @PathVariable UUID id, @RequestBody UpdateActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(activityService.update(id, principal.getOrgId(), request)));
    }

    @Operation(summary = "Deactivate an activity (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        activityService.deactivate(id, principal.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Magic Fill ──────────────────────────────────────────────────────────

    @Operation(summary = "Whether AI magic fill is configured for this org (no key exposed)")
    @GetMapping("/ai-status")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<AiStatusResponse>> aiStatus(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(new AiStatusResponse(magicFillService.isEnabled(principal.getOrgId()))));
    }

    @Operation(summary = "AI-draft Instructions or a Checklist for an activity being authored")
    @PostMapping("/magic-fill")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<MagicFillResponse>> magicFill(
            @Valid @RequestBody MagicFillRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(magicFillService.generate(principal.getOrgId(), request)));
    }

    public record AiStatusResponse(boolean enabled) {}

    // ── Shared library ──────────────────────────────────────────────────────

    @Operation(summary = "Browse activities other orgs have shared")
    @GetMapping("/shared-library")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> sharedLibrary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(sharingService.sharedLibrary(principal.getOrgId())));
    }

    @Operation(summary = "Clone a shared activity into this org")
    @PostMapping("/{id}/import")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ActivityResponse>> importActivity(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        ActivityResponse imported = sharingService.importActivity(id, principal.getOrgId(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(imported));
    }

    // ── Resources (file uploads) ───────────────────────────────────────────

    @Operation(summary = "Upload a resource file to an activity")
    @PostMapping("/{id}/resources")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ActivityResourceResponse>> uploadResource(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Activity activity = activityService.requireOwned(id, principal.getOrgId());
        String url = storageService.store(file, "activities/" + id);

        ActivityResource resource = new ActivityResource();
        resource.setOrgId(activity.getOrgId());
        resource.setActivityId(activity.getId());
        resource.setUploadedBy(principal.getId());
        resource.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        resource.setFileUrl(url);
        resource.setContentType(file.getContentType());
        resource.setFileSizeBytes(file.getSize());

        ActivityResource saved = resourceRepository.save(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ActivityResourceResponse.from(saved)));
    }

    @Operation(summary = "Delete an activity resource")
    @DeleteMapping("/{id}/resources/{resourceId}")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable UUID id, @PathVariable UUID resourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        activityService.requireOwned(id, principal.getOrgId());
        ActivityResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (!resource.getActivityId().equals(id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        storageService.delete(resource.getFileUrl());
        resourceRepository.delete(resource);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Assignments ─────────────────────────────────────────────────────────

    @Operation(summary = "Assign an activity to a patient")
    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ")")
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityAssignmentResponse assigned = activityService.assign(id, principal.getOrgId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assigned));
    }

    @Operation(summary = "List activity assignments for a patient")
    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'PARENT')")
    public ResponseEntity<ApiResponse<List<ActivityAssignmentResponse>>> listAssignments(
            @RequestParam UUID patientId, @AuthenticationPrincipal UserPrincipal principal) {
        requirePatientAccess(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                activityService.listAssignmentsForPatient(principal.getOrgId(), patientId)));
    }

    @Operation(summary = "Update an assignment's status")
    @PatchMapping("/assignments/{assignmentId}/status")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ")")
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> updateStatus(
            @PathVariable UUID assignmentId, @Valid @RequestBody UpdateAssignmentStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                activityService.updateAssignmentStatus(assignmentId, principal.getOrgId(), request.status())));
    }

    // ── Attempts ────────────────────────────────────────────────────────────

    @Operation(summary = "Log one checklist attempt against an assignment")
    @PostMapping("/assignments/{assignmentId}/attempts")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ")")
    public ResponseEntity<ApiResponse<ActivityAttemptResponse>> logAttempt(
            @PathVariable UUID assignmentId, @Valid @RequestBody LogAttemptRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityAttemptResponse logged = activityService.logAttempt(assignmentId, principal.getOrgId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(logged));
    }

    @Operation(summary = "List attempt history for an assignment")
    @GetMapping("/assignments/{assignmentId}/attempts")
    @PreAuthorize("hasAnyRole(" + STAFF_ROLES + ", 'PARENT')")
    public ResponseEntity<ApiResponse<List<ActivityAttemptResponse>>> listAttempts(
            @PathVariable UUID assignmentId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(activityService.listAttempts(assignmentId, principal.getOrgId())));
    }

    // ── Access helpers ──────────────────────────────────────────────────────

    private void requirePatientAccess(UUID patientId, UserPrincipal principal) {
        boolean isParent = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));
        if (!isParent) return;
        boolean linked = patientParentRepository.findById_ParentId(principal.getId()).stream()
                .anyMatch(pp -> pp.getId().getPatientId().equals(patientId));
        if (!linked) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}

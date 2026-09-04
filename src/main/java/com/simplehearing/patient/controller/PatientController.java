package com.simplehearing.patient.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.dto.PagedResponse;
import com.simplehearing.patient.dto.*;
import com.simplehearing.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Patients", description = "Patient management, conditions, parent links, therapist assignments")
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a patient")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> create(
            @Valid @RequestBody CreatePatientRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(patientService.create(request, principal)));
    }

    @Operation(
        summary = "List patients in your organisation, paginated",
        description = "Defaults to 20 per page, sorted by createdAt (year joined) descending. " +
                      "`status` is a comma-separated subset of ACTIVE,INACTIVE (Active = not discharged, " +
                      "Inactive = stage DISCHARGED) — omitted defaults to ACTIVE; an explicitly empty value " +
                      "returns every status. `mine` scopes to " +
                      "patients assigned to the caller (always on for THERAPIST, regardless of this param). " +
                      "`compact=true` returns parents/therapists as id-only stubs (blank name/email) — for " +
                      "callers that only read .length (e.g. the Cases page's invite-status pill and specialist " +
                      "count), not their names; leave false for callers that display names (e.g. Dashboard's " +
                      "Recently Joined therapist filter)."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean compact,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                patientService.listForOrg(search, mine, status, compact, pageable, principal)));
    }

    @Operation(summary = "Patients whose birthday falls in the next 30 days")
    @GetMapping("/upcoming-birthdays")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<UpcomingBirthdayResponse>>> upcomingBirthdays(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.upcomingBirthdays(principal)));
    }

    @Operation(summary = "List patients where I am a linked parent (my children)")
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> myChildren(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.listMyChildren(principal)));
    }

    @Operation(summary = "Get a patient by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.get(id, principal)));
    }

    @Operation(summary = "Update a patient")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> update(
            @PathVariable UUID id,
            @RequestBody CreatePatientRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.update(id, request, principal)));
    }

    @Operation(summary = "Update patient journey stage")
    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> updateStage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientStageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.updateStage(id, request, principal)));
    }

    @Operation(summary = "Delete a patient and all associated records")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        patientService.delete(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    @Operation(summary = "Add a condition to a patient")
    @PostMapping("/{id}/conditions")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> addCondition(
            @PathVariable UUID id,
            @Valid @RequestBody AddConditionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.addCondition(id, request, principal)));
    }

    @Operation(summary = "Remove a condition from a patient")
    @DeleteMapping("/{id}/conditions/{conditionId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<Void> removeCondition(
            @PathVariable UUID id,
            @PathVariable UUID conditionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        patientService.removeCondition(id, conditionId, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Parents ───────────────────────────────────────────────────────────────

    @Operation(summary = "Link a parent user to a patient")
    @PostMapping("/{id}/parents")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> linkParent(
            @PathVariable UUID id,
            @Valid @RequestBody LinkParentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.linkParent(id, request, principal)));
    }

    @Operation(summary = "Unlink a parent from a patient")
    @DeleteMapping("/{id}/parents/{parentId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<Void> unlinkParent(
            @PathVariable UUID id,
            @PathVariable UUID parentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        patientService.unlinkParent(id, parentId, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Invite a parent by email who doesn't have an account yet; auto-linked to this patient on accept",
               description = "If the email already belongs to an active account in this org, no invite is sent — "
                           + "the response's existingUser carries that account's summary instead, for the caller "
                           + "to confirm before POSTing it to /parents/link-existing-user.")
    @PostMapping("/{id}/parents/invite")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InviteParentResponse>> inviteParent(
            @PathVariable UUID id,
            @Valid @RequestBody InviteParentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(patientService.inviteParent(id, request, principal)));
    }

    @Operation(summary = "Link an existing org member as a patient's parent",
               description = "Grants Parent access to a Member who already has an account under a different role "
                           + "(e.g. a Therapist) — adds PARENT to their additionalRoles if they don't already have "
                           + "it, without touching their primary role. Confirms the existingUser from POST "
                           + "/parents/invite.")
    @PostMapping("/{id}/parents/link-existing-user")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> linkExistingUserAsParent(
            @PathVariable UUID id,
            @Valid @RequestBody LinkParentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.linkExistingUserAsParent(id, request, principal)));
    }

    // ── Therapist assignments ─────────────────────────────────────────────────

    @Operation(summary = "Assign a therapist to a patient")
    @PostMapping("/{id}/therapists")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> assignTherapist(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTherapistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.assignTherapist(id, request, principal)));
    }

    @Operation(summary = "Unassign a therapist from a patient")
    @DeleteMapping("/{id}/therapists/{therapistId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<Void> unassignTherapist(
            @PathVariable UUID id,
            @PathVariable UUID therapistId,
            @AuthenticationPrincipal UserPrincipal principal) {
        patientService.unassignTherapist(id, therapistId, principal);
        return ResponseEntity.noContent().build();
    }
}

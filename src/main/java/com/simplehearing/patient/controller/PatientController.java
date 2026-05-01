package com.simplehearing.patient.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.patient.dto.*;
import com.simplehearing.patient.service.PatientService;
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
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<PatientResponse>> create(
            @Valid @RequestBody CreatePatientRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(patientService.create(request, principal)));
    }

    @Operation(summary = "List all patients in your organisation")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.listForOrg(principal)));
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.get(id, principal)));
    }

    @Operation(summary = "Update a patient")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<PatientResponse>> update(
            @PathVariable UUID id,
            @RequestBody CreatePatientRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.update(id, request, principal)));
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    @Operation(summary = "Add a condition to a patient")
    @PostMapping("/{id}/conditions")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<PatientResponse>> addCondition(
            @PathVariable UUID id,
            @Valid @RequestBody AddConditionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.addCondition(id, request, principal)));
    }

    @Operation(summary = "Remove a condition from a patient")
    @DeleteMapping("/{id}/conditions/{conditionId}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
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
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<PatientResponse>> linkParent(
            @PathVariable UUID id,
            @Valid @RequestBody LinkParentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.linkParent(id, request, principal)));
    }

    @Operation(summary = "Unlink a parent from a patient")
    @DeleteMapping("/{id}/parents/{parentId}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> unlinkParent(
            @PathVariable UUID id,
            @PathVariable UUID parentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        patientService.unlinkParent(id, parentId, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Therapist assignments ─────────────────────────────────────────────────

    @Operation(summary = "Assign a therapist to a patient")
    @PostMapping("/{id}/therapists")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<PatientResponse>> assignTherapist(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTherapistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(patientService.assignTherapist(id, request, principal)));
    }

    @Operation(summary = "Unassign a therapist from a patient")
    @DeleteMapping("/{id}/therapists/{therapistId}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> unassignTherapist(
            @PathVariable UUID id,
            @PathVariable UUID therapistId,
            @AuthenticationPrincipal UserPrincipal principal) {
        patientService.unassignTherapist(id, therapistId, principal);
        return ResponseEntity.noContent().build();
    }
}

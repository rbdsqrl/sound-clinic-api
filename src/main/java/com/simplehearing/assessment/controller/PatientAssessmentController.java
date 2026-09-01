package com.simplehearing.assessment.controller;

import com.simplehearing.assessment.dto.AssessmentDefinitionResponse;
import com.simplehearing.assessment.dto.CreateAssessmentRequest;
import com.simplehearing.assessment.dto.PatientAssessmentResponse;
import com.simplehearing.assessment.service.AssessmentService;
import com.simplehearing.auth.security.UserPrincipal;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Patient Assessments", description = "Clinical assessment fills (ISAA, PRBA, M-CHAT-R, ADL, Pre Assessment Form), per patient, over time")
@RestController
@RequestMapping("/api/v1/patients/{patientId}/assessments/{type}")
public class PatientAssessmentController {

    private final AssessmentService assessmentService;
    private final PatientParentRepository patientParentRepository;

    public PatientAssessmentController(AssessmentService assessmentService, PatientParentRepository patientParentRepository) {
        this.assessmentService = assessmentService;
        this.patientParentRepository = patientParentRepository;
    }

    @Operation(summary = "Get an assessment type's category/item/option definition")
    @GetMapping("/definition")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<AssessmentDefinitionResponse>> getDefinition(
            @PathVariable UUID patientId,
            @PathVariable String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(assessmentService.getDefinition(type)));
    }

    @Operation(summary = "List a patient's fills of this assessment, oldest first")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<List<PatientAssessmentResponse>>> list(
            @PathVariable UUID patientId,
            @PathVariable String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.list(principal.getOrgId(), patientId, type)));
    }

    @Operation(summary = "Download one filled assessment as a PDF, laid out like the paper form")
    @GetMapping("/{assessmentId}/pdf")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> pdf(
            @PathVariable UUID patientId,
            @PathVariable String type,
            @PathVariable UUID assessmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        String url = assessmentService.generatePdfUrl(principal.getOrgId(), patientId, assessmentId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @Operation(summary = "Record a new fill of this assessment for a patient")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<PatientAssessmentResponse>> create(
            @PathVariable UUID patientId,
            @PathVariable String type,
            @Valid @RequestBody CreateAssessmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PatientAssessmentResponse created = assessmentService.create(
                principal.getOrgId(), patientId, principal.getId(), type, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Upload a file for a FILE-type item (e.g. a prior assessment report) and get back a URL to submit in the fill")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @PathVariable UUID patientId,
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        String url = assessmentService.uploadFile(patientId, file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

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

package com.simplehearing.program.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.program.dto.CreateProgramRequest;
import com.simplehearing.program.dto.ProgramResponse;
import com.simplehearing.program.dto.UpdateProgramRequest;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.feedback.dto.ProgramFeedbackQuestionResponse;
import com.simplehearing.program.feedback.dto.UpdateProgramFeedbackTemplateRequest;
import com.simplehearing.program.feedback.service.ProgramFeedbackService;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.tax.entity.Tax;
import com.simplehearing.tax.repository.TaxRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Programs", description = "Therapy program catalog management")
@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

    private final ProgramRepository programRepository;
    private final TaxRepository taxRepository;
    private final ProgramFeedbackService programFeedbackService;

    public ProgramController(ProgramRepository programRepository, TaxRepository taxRepository,
                              ProgramFeedbackService programFeedbackService) {
        this.programRepository = programRepository;
        this.taxRepository = taxRepository;
        this.programFeedbackService = programFeedbackService;
    }

    // ── List all programs (includes inactive) ─────────────────────────────────

    @Operation(summary = "List all programs for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgramResponse>>> list(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Program> programs = activeOnly
                ? programRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                : programRepository.findByOrgIdOrderByNameAsc(principal.getOrgId());

        Map<UUID, Tax> taxesById = resolveTaxes(programs);

        List<ProgramResponse> result = programs.stream()
                .map(p -> ProgramResponse.from(p, taxesById.get(p.getTaxId())))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Create a program ───────────────────────────────────────────────────────

    @Operation(summary = "Create a new therapy program")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> create(
            @Valid @RequestBody CreateProgramRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Tax tax = null;
        if (request.taxId() != null) {
            tax = taxRepository.findById(request.taxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));
            if (!tax.getOrgId().equals(principal.getOrgId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        Program program = new Program();
        program.setOrgId(principal.getOrgId());
        program.setName(request.name().trim());
        program.setDescription(request.description());
        program.setPerSessionCost(request.perSessionCost());
        program.setTaxId(tax != null ? tax.getId() : null);
        program.setPriceIncludesTax(tax != null && request.priceIncludesTax() != null ? request.priceIncludesTax() : true);
        program.setCreatedBy(principal.getId());

        Program saved = programRepository.save(program);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ProgramResponse.from(saved, tax)));
    }

    // ── Update a program ───────────────────────────────────────────────────────

    @Operation(summary = "Update program name, cost, or active status")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateProgramRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        if (!program.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (request.name() != null && !request.name().isBlank()) {
            program.setName(request.name().trim());
        }
        if (request.description() != null) {
            program.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
        if (request.perSessionCost() != null) {
            program.setPerSessionCost(request.perSessionCost());
        }
        if (Boolean.TRUE.equals(request.removeTax())) {
            program.setTaxId(null);
            program.setPriceIncludesTax(true);
        } else if (request.taxId() != null) {
            Tax tax = taxRepository.findById(request.taxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));
            if (!tax.getOrgId().equals(principal.getOrgId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
            }
            program.setTaxId(tax.getId());
            program.setPriceIncludesTax(request.priceIncludesTax() != null ? request.priceIncludesTax() : true);
        }
        if (request.isActive() != null) {
            program.setActive(request.isActive());
        }

        Program saved = programRepository.save(program);
        Tax tax = saved.getTaxId() != null ? taxRepository.findById(saved.getTaxId()).orElse(null) : null;
        return ResponseEntity.ok(ApiResponse.success(ProgramResponse.from(saved, tax)));
    }

    // ── Deactivate a program (soft delete) ────────────────────────────────────

    @Operation(summary = "Deactivate a program (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        if (!program.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        program.setActive(false);
        Program saved = programRepository.save(program);
        Tax tax = saved.getTaxId() != null ? taxRepository.findById(saved.getTaxId()).orElse(null) : null;
        return ResponseEntity.ok(ApiResponse.success(ProgramResponse.from(saved, tax)));
    }

    // ── Session feedback template ──────────────────────────────────────────────

    @Operation(summary = "Get a program's session feedback checklist template")
    @GetMapping("/{id}/feedback-template")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgramFeedbackQuestionResponse>>> getFeedbackTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        if (!program.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok(ApiResponse.success(programFeedbackService.getTemplate(id)));
    }

    @Operation(summary = "Replace a program's session feedback checklist template")
    @PutMapping("/{id}/feedback-template")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgramFeedbackQuestionResponse>>> updateFeedbackTemplate(
            @PathVariable UUID id,
            @RequestBody UpdateProgramFeedbackTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        if (!program.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok(ApiResponse.success(
                programFeedbackService.replaceTemplate(id, principal.getOrgId(), request)));
    }

    private Map<UUID, Tax> resolveTaxes(List<Program> programs) {
        List<UUID> taxIds = programs.stream()
                .map(Program::getTaxId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (taxIds.isEmpty()) {
            return new HashMap<>();
        }
        return taxRepository.findAllById(taxIds).stream()
                .collect(Collectors.toMap(Tax::getId, t -> t));
    }
}

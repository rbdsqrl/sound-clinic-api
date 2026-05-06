package com.simplehearing.program.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.program.dto.CreateProgramRequest;
import com.simplehearing.program.dto.ProgramResponse;
import com.simplehearing.program.dto.UpdateProgramRequest;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
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

@Tag(name = "Programs", description = "Therapy program catalog management")
@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

    private final ProgramRepository programRepository;

    public ProgramController(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    // ── List all programs (includes inactive) ─────────────────────────────────

    @Operation(summary = "List all programs for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgramResponse>>> list(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Program> programs = activeOnly
                ? programRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                : programRepository.findByOrgIdOrderByNameAsc(principal.getOrgId());

        List<ProgramResponse> result = programs.stream()
                .map(ProgramResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Create a program ───────────────────────────────────────────────────────

    @Operation(summary = "Create a new therapy program")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> create(
            @Valid @RequestBody CreateProgramRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Program program = new Program();
        program.setOrgId(principal.getOrgId());
        program.setName(request.name().trim());
        program.setPerSessionCost(request.perSessionCost());
        program.setCreatedBy(principal.getId());

        Program saved = programRepository.save(program);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ProgramResponse.from(saved)));
    }

    // ── Update a program ───────────────────────────────────────────────────────

    @Operation(summary = "Update program name, cost, or active status")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
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
        if (request.perSessionCost() != null) {
            program.setPerSessionCost(request.perSessionCost());
        }
        if (request.isActive() != null) {
            program.setActive(request.isActive());
        }

        Program saved = programRepository.save(program);
        return ResponseEntity.ok(ApiResponse.success(ProgramResponse.from(saved)));
    }

    // ── Deactivate a program (soft delete) ────────────────────────────────────

    @Operation(summary = "Deactivate a program (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
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
        return ResponseEntity.ok(ApiResponse.success(ProgramResponse.from(saved)));
    }
}

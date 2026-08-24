package com.simplehearing.activity.controller;

import com.simplehearing.activity.dto.SkillResponse;
import com.simplehearing.activity.entity.Skill;
import com.simplehearing.activity.repository.SkillRepository;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Activity Skills", description = "Per-org skill tags used on activities")
@RestController
@RequestMapping("/api/v1/activities/skills")
public class SkillController {

    private final SkillRepository skillRepository;

    public SkillController(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Operation(summary = "List all active skills for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST')")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        List<SkillResponse> results = skillRepository
                .findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                .stream().map(SkillResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Add a skill for this org")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<SkillResponse>> create(
            @RequestBody CreateSkillRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Skill skill = new Skill();
        skill.setOrgId(principal.getOrgId());
        skill.setName(request.name().trim());
        Skill saved = skillRepository.save(skill);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SkillResponse.from(saved)));
    }

    @Operation(summary = "Delete a skill")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        if (!skill.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        skillRepository.delete(skill);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record CreateSkillRequest(@NotBlank String name) {}
}

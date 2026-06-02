package com.simplehearing.condition.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.condition.dto.ConditionResponse;
import com.simplehearing.condition.entity.Condition;
import com.simplehearing.condition.repository.ConditionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Tag(name = "Conditions", description = "Per-org conditions list management")
@RestController
@RequestMapping("/api/v1/conditions")
public class ConditionController {

    private final ConditionRepository conditionRepository;

    public ConditionController(ConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    @Operation(summary = "List all active conditions (global + org-specific)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConditionResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ConditionResponse> results = new ArrayList<>();
        if (principal != null) {
            conditionRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                    .stream().map(ConditionResponse::from).forEach(results::add);
        }
        conditionRepository.findByOrgIdIsNullAndIsActiveTrueOrderByNameAsc()
                .stream().map(ConditionResponse::from).forEach(results::add);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Add a new condition for this org")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ConditionResponse>> create(
            @RequestBody CreateConditionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Condition condition = new Condition();
        condition.setOrgId(principal.getOrgId());
        condition.setName(request.name().trim());
        Condition saved = conditionRepository.save(condition);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ConditionResponse.from(saved)));
    }

    @Operation(summary = "Delete an org condition")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));

        if (condition.getOrgId() == null || !condition.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot delete a global condition");
        }

        conditionRepository.delete(condition);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record CreateConditionRequest(@NotBlank String name) {}
}

package com.simplehearing.condition.controller;

import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.condition.dto.ConditionResponse;
import com.simplehearing.condition.repository.ConditionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Conditions", description = "Reference list of medical conditions")
@RestController
@RequestMapping("/api/v1/conditions")
public class ConditionController {

    private final ConditionRepository conditionRepository;

    public ConditionController(ConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    @Operation(summary = "List all active conditions")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConditionResponse>>> list() {
        List<ConditionResponse> conditions = conditionRepository.findByIsActiveTrue().stream()
                .map(ConditionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(conditions));
    }
}

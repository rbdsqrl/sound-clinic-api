package com.simplehearing.auditlog.controller;

import com.simplehearing.auditlog.dto.AuditLogResponse;
import com.simplehearing.auditlog.repository.AuditLogEntryRepository;
import com.simplehearing.auditlog.service.AuditLogService;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit-log")
@Tag(name = "Activity Log")
public class AuditLogController {

    private static final int MAX_ENTRIES = 200;

    private final AuditLogEntryRepository repository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuditLogController(AuditLogEntryRepository repository,
                              AuditLogService auditLogService,
                              UserRepository userRepository) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Operation(
        summary = "Activity log of a member, parent or case — BUSINESS_OWNER only",
        description = "Newest first. subjectType is USER (a member or a parent) or CASE; subjectId is the "
                    + "user's or patient's id. Returns at most 200 entries."
    )
    @GetMapping
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> list(
            @RequestParam String subjectType,
            @RequestParam UUID subjectId,
            @AuthenticationPrincipal UserPrincipal principal) {

        String type = subjectType.toUpperCase();
        if (!type.equals(AuditLogService.USER) && !type.equals(AuditLogService.CASE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "subjectType must be USER or CASE");
        }

        var entries = repository.findByOrgIdAndSubjectTypeAndSubjectIdOrderByCreatedAtDesc(
                principal.getOrgId(), type, subjectId, PageRequest.of(0, MAX_ENTRIES));

        Set<UUID> actorIds = new HashSet<>();
        entries.forEach(e -> { if (e.getActorId() != null) actorIds.add(e.getActorId()); });
        Map<UUID, User> actors = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AuditLogResponse> result = entries.stream().map(e -> {
            String name;
            if (e.getActorId() == null) {
                name = "System";
            } else {
                User u = actors.get(e.getActorId());
                name = u != null ? (u.getFirstName() + " " + u.getLastName()).trim() : "Unknown";
            }
            return new AuditLogResponse(e.getId(), e.getActorId(), name, e.getEventType(), e.getSummary(),
                    auditLogService.parseChanges(e.getChanges()), e.getCreatedAt());
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

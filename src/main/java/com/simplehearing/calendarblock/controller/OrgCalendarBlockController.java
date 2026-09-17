package com.simplehearing.calendarblock.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.calendarblock.dto.CreateOrgCalendarBlockRequest;
import com.simplehearing.calendarblock.dto.OrgCalendarBlockResponse;
import com.simplehearing.calendarblock.entity.OrgCalendarBlock;
import com.simplehearing.calendarblock.repository.OrgCalendarBlockRepository;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
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

@Tag(name = "Org Calendar Blocks", description = "Org-wide recurring calendar blocks shown on every user's calendar (e.g. Lunch Break)")
@RestController
@RequestMapping("/api/v1/calendar-blocks")
public class OrgCalendarBlockController {

    private final OrgCalendarBlockRepository blockRepository;

    public OrgCalendarBlockController(OrgCalendarBlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Operation(summary = "List the organisation's recurring calendar blocks — visible to every authenticated user, same as the calendar itself")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrgCalendarBlockResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<OrgCalendarBlockResponse> result = blockRepository.findByOrgIdOrderByStartTimeAsc(principal.getOrgId())
                .stream().map(OrgCalendarBlockResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Add a recurring calendar block")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<OrgCalendarBlockResponse>> create(
            @Valid @RequestBody CreateOrgCalendarBlockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be on or after the start date");
        }

        OrgCalendarBlock block = new OrgCalendarBlock();
        block.setOrgId(principal.getOrgId());
        block.setTitle(request.title());
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());
        block.setDaysOfWeek(request.daysOfWeek());
        block.setStartDate(request.startDate());
        block.setEndDate(request.endDate());
        block.setCreatedBy(principal.getId());
        OrgCalendarBlock saved = blockRepository.save(block);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(OrgCalendarBlockResponse.from(saved)));
    }

    @Operation(summary = "Delete a recurring calendar block")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrgCalendarBlock block = blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar block not found"));

        if (!block.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        blockRepository.delete(block);
        return ResponseEntity.noContent().build();
    }
}

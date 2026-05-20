package com.simplehearing.holiday.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ConflictException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.holiday.dto.CreatePublicHolidayRequest;
import com.simplehearing.holiday.dto.PublicHolidayResponse;
import com.simplehearing.holiday.entity.PublicHoliday;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.RescheduleReason;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
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

@Tag(name = "Public Holidays", description = "Manage organisation public holidays")
@RestController
@RequestMapping("/api/v1/public-holidays")
public class PublicHolidayController {

    private final PublicHolidayRepository holidayRepository;
    private final TherapySessionRepository sessionRepository;

    public PublicHolidayController(PublicHolidayRepository holidayRepository,
                                   TherapySessionRepository sessionRepository) {
        this.holidayRepository = holidayRepository;
        this.sessionRepository = sessionRepository;
    }

    @Operation(summary = "List all public holidays for the organisation")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<PublicHolidayResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PublicHoliday> holidays = holidayRepository.findByOrgIdOrderByHolidayDateAsc(principal.getOrgId());
        List<PublicHolidayResponse> result = holidays.stream()
                .map(h -> PublicHolidayResponse.from(h, 0))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Add a public holiday and flag scheduled sessions on that date as needing reschedule")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicHolidayResponse>> create(
            @Valid @RequestBody CreatePublicHolidayRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (holidayRepository.findByOrgIdAndHolidayDate(principal.getOrgId(), request.holidayDate()).isPresent()) {
            throw new ConflictException("A public holiday already exists for that date");
        }

        PublicHoliday holiday = new PublicHoliday();
        holiday.setOrgId(principal.getOrgId());
        holiday.setHolidayDate(request.holidayDate());
        holiday.setName(request.name());
        holiday.setCreatedBy(principal.getId());
        PublicHoliday saved = holidayRepository.save(holiday);

        // Flag all SCHEDULED sessions on this date as PENDING_RESCHEDULE
        List<TherapySession> affected = sessionRepository
                .findByOrgIdAndSessionDateAndStatus(
                        principal.getOrgId(), request.holidayDate(), TherapySessionStatus.SCHEDULED);
        affected.forEach(s -> {
            s.setStatus(TherapySessionStatus.PENDING_RESCHEDULE);
            s.setRescheduleReason(RescheduleReason.PUBLIC_HOLIDAY);
        });
        sessionRepository.saveAll(affected);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PublicHolidayResponse.from(saved, affected.size())));
    }

    @Operation(summary = "Delete a public holiday (does not auto-restore flagged sessions)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        PublicHoliday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public holiday not found"));

        if (!holiday.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        holidayRepository.delete(holiday);
        return ResponseEntity.noContent().build();
    }
}

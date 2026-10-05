package com.simplehearing.leave.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.leave.dto.LeavePolicyDtos.*;
import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.entity.LeaveAllocation;
import com.simplehearing.leave.entity.LeaveCategory;
import com.simplehearing.leave.enums.LeaveStatus;
import com.simplehearing.leave.repository.LeaveAllocationRepository;
import com.simplehearing.leave.repository.LeaveCategoryRepository;
import com.simplehearing.leave.repository.LeaveRepository;
import com.simplehearing.leave.service.LeavePolicyService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Leave Policy", description = "Leave categories, yearly allocations and balances")
@RestController
@RequestMapping("/api/v1/leave-policy")
public class LeavePolicyController {

    private static final List<Role> STAFF_ROLES = List.of(Role.THERAPIST, Role.CLINIC_HEAD, Role.OFFICE_ADMIN, Role.BUSINESS_OWNER);

    private final LeavePolicyService policy;
    private final LeaveCategoryRepository categoryRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveRepository leaveRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;

    public LeavePolicyController(LeavePolicyService policy, LeaveCategoryRepository categoryRepository,
                                 LeaveAllocationRepository allocationRepository, LeaveRepository leaveRepository,
                                 OrganisationRepository organisationRepository, UserRepository userRepository) {
        this.policy = policy;
        this.categoryRepository = categoryRepository;
        this.allocationRepository = allocationRepository;
        this.leaveRepository = leaveRepository;
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
    }

    private static CategoryResponse toResponse(LeaveCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getAnnualDays(), c.isActive());
    }

    // ── Categories ───────────────────────────────────────────────────────────

    @Operation(summary = "Leave categories — active ones for everyone; pass all=true (admin roles) to include deactivated ones")
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN', 'THERAPIST')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories(
            @RequestParam(defaultValue = "false") boolean all,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean admin = principal.getActiveRole() != Role.THERAPIST;
        List<LeaveCategory> list = (all && admin)
                ? categoryRepository.findByOrgIdOrderByNameAsc(principal.getOrgId())
                : categoryRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(list.stream().map(LeavePolicyController::toResponse).toList()));
    }

    @Operation(summary = "Add a leave category")
    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest body, @AuthenticationPrincipal UserPrincipal principal) {
        String name = body.name().trim();
        if (categoryRepository.existsByOrgIdAndNameIgnoreCase(principal.getOrgId(), name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A leave category called \"" + name + "\" already exists");
        }
        LeaveCategory c = new LeaveCategory();
        c.setOrgId(principal.getOrgId());
        c.setName(name);
        c.setAnnualDays(body.annualDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(categoryRepository.save(c))));
    }

    @Operation(summary = "Rename a leave category, change its yearly allowance, or deactivate it (leaves already taken keep the label)")
    @PatchMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest body, @AuthenticationPrincipal UserPrincipal principal) {
        LeaveCategory c = categoryRepository.findByIdAndOrgId(id, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave category not found"));
        if (body.name() != null && !body.name().isBlank() && !body.name().trim().equalsIgnoreCase(c.getName())) {
            if (categoryRepository.existsByOrgIdAndNameIgnoreCase(principal.getOrgId(), body.name().trim())) {
                throw new ApiException(HttpStatus.CONFLICT, "A leave category called \"" + body.name().trim() + "\" already exists");
            }
            c.setName(body.name().trim());
        } else if (body.name() != null && !body.name().isBlank()) {
            c.setName(body.name().trim());   // same name, different case
        }
        if (Boolean.TRUE.equals(body.clearAnnualDays())) c.setAnnualDays(null);
        else if (body.annualDays() != null) c.setAnnualDays(body.annualDays());
        if (body.active() != null) c.setActive(body.active());
        return ResponseEntity.ok(ApiResponse.success(toResponse(categoryRepository.save(c))));
    }

    // ── Leave year setting ───────────────────────────────────────────────────

    @Operation(summary = "The month the leave year starts (1 = a calendar year, 4 = April to March)")
    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN', 'THERAPIST')")
    public ResponseEntity<ApiResponse<SettingsDto>> getSettings(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(new SettingsDto(policy.yearStartMonth(principal.getOrgId()))));
    }

    @Operation(summary = "Set the month the leave year starts")
    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<SettingsDto>> updateSettings(
            @Valid @RequestBody SettingsDto body, @AuthenticationPrincipal UserPrincipal principal) {
        Organisation org = organisationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organisation not found"));
        org.setLeaveYearStartMonth(body.yearStartMonth());
        organisationRepository.save(org);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    // ── Balances ─────────────────────────────────────────────────────────────

    @Operation(summary = "My leave balances for a leave year (defaults to the current one)")
    @GetMapping("/my-balances")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN', 'THERAPIST')")
    public ResponseEntity<ApiResponse<MyBalancesResponse>> myBalances(
            @RequestParam(required = false) Integer year, @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        int y = year != null ? year : policy.leaveYearOf(orgId, LocalDate.now());
        List<Balance> balances = policy.balances(orgId, List.of(principal.getId()), y).getOrDefault(principal.getId(), List.of());
        return ResponseEntity.ok(ApiResponse.success(new MyBalancesResponse(y, policy.yearStart(orgId, y), policy.yearEnd(orgId, y), balances)));
    }

    @Operation(summary = "Everyone's leave balances and request counts by approval status for a leave year")
    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<BalancesResponse>> balances(
            @RequestParam(required = false) Integer year, @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        int y = year != null ? year : policy.leaveYearOf(orgId, LocalDate.now());
        LocalDate start = policy.yearStart(orgId, y), end = policy.yearEnd(orgId, y);

        List<User> staff = userRepository.findByOrgIdAndRoleIn(orgId, STAFF_ROLES).stream().filter(User::isActive).toList();
        List<UUID> ids = staff.stream().map(User::getId).toList();
        Map<UUID, List<Balance>> balances = policy.balances(orgId, ids, y);

        Map<UUID, int[]> counts = new HashMap<>();   // {pending, approved, rejected}
        if (!ids.isEmpty()) {
            for (Leave l : leaveRepository.findOverlapping(orgId, ids, List.of(LeaveStatus.values()), start, end)) {
                int[] c = counts.computeIfAbsent(l.getTherapistId(), k -> new int[3]);
                c[l.getStatus() == LeaveStatus.PENDING ? 0 : l.getStatus() == LeaveStatus.APPROVED ? 1 : 2]++;
            }
        }

        List<PersonBalances> people = staff.stream()
                .sorted(Comparator.comparing((User u) -> (u.getFirstName() + " " + u.getLastName()).toLowerCase()))
                .map(u -> {
                    int[] c = counts.getOrDefault(u.getId(), new int[3]);
                    return new PersonBalances(u.getId(), (u.getFirstName() + " " + u.getLastName()).trim(), u.getRole().name(),
                            balances.getOrDefault(u.getId(), List.of()), c[0], c[1], c[2]);
                }).toList();
        return ResponseEntity.ok(ApiResponse.success(new BalancesResponse(y, start, end, people)));
    }

    // ── Per-person allocations ───────────────────────────────────────────────

    @Operation(summary = "Set one person's allocation for a category in a leave year (overrides the category default); days = null restores the default")
    @PutMapping("/allocations")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> upsertAllocation(
            @Valid @RequestBody UpsertAllocationRequest body, @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        userRepository.findById(body.userId()).filter(u -> orgId.equals(u.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        categoryRepository.findByIdAndOrgId(body.categoryId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave category not found"));

        Optional<LeaveAllocation> existing = allocationRepository.findByUserIdAndCategoryIdAndLeaveYear(body.userId(), body.categoryId(), body.year());
        if (body.days() == null) {
            existing.ifPresent(allocationRepository::delete);
        } else {
            LeaveAllocation a = existing.orElseGet(LeaveAllocation::new);
            a.setOrgId(orgId);
            a.setUserId(body.userId());
            a.setCategoryId(body.categoryId());
            a.setLeaveYear(body.year());
            a.setDays(body.days());
            allocationRepository.save(a);
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

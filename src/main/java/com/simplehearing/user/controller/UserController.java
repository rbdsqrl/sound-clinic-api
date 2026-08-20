package com.simplehearing.user.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.user.dto.AssignableUserResponse;
import com.simplehearing.user.dto.StaffMemberResponse;
import com.simplehearing.user.dto.UserResponse;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Users", description = "User profile and role management")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /** Roles that can be added as additional (secondary) roles. */
    private static final Set<Role> GRANTABLE_ADDITIONAL_ROLES = Set.of(Role.PARENT);

    /** Primary roles allowed to acquire an additional role. */
    private static final Set<Role> ELIGIBLE_PRIMARY_ROLES = Set.of(Role.BUSINESS_OWNER, Role.THERAPIST, Role.DOCTOR);

    private final UserRepository userRepository;
    private final TherapistPatientRepository therapistPatientRepository;

    public UserController(UserRepository userRepository,
                          TherapistPatientRepository therapistPatientRepository) {
        this.userRepository = userRepository;
        this.therapistPatientRepository = therapistPatientRepository;
    }

    /** Roles that count as "clinical staff" for the therapists list. */
    private static final List<Role> CLINICAL_ROLES = List.of(Role.THERAPIST, Role.DOCTOR);

    /** Everyone who works at the clinic — as opposed to parents and patients. */
    private static final List<Role> STAFF_ROLES = List.of(
            Role.ADMIN, Role.BUSINESS_OWNER, Role.OFFICE_ADMIN, Role.THERAPIST, Role.DOCTOR);

    @Operation(summary = "List all staff members in the organisation")
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffMemberResponse>>> listMembers(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<User> staff = userRepository.findByOrgIdAndRoleIn(principal.getOrgId(), STAFF_ROLES)
                .stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .toList();

        List<UUID> therapistIds = staff.stream().map(User::getId).toList();
        Map<UUID, Long> caseCountByTherapist = therapistPatientRepository
                .countCasesByTherapistIds(therapistIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        List<StaffMemberResponse> results = staff.stream()
                .map(u -> StaffMemberResponse.from(u,
                        caseCountByTherapist.getOrDefault(u.getId(), 0L).intValue()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(
        summary = "List staff who can be assigned work",
        description = "Names and roles only — used to populate assignee pickers. Any staff member may read it, "
                    + "since anyone can create a task and assign it; personal details are deliberately left out."
    )
    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<AssignableUserResponse>>> listAssignable(
            @RequestParam(defaultValue = "false") boolean includeParents,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Meeting participant pickers need parents as well as staff. Still names and
        // roles only, so this stays safe to expose to every staff member.
        List<Role> roles = includeParents
                ? Stream.concat(STAFF_ROLES.stream(), Stream.of(Role.PARENT)).toList()
                : STAFF_ROLES;

        List<AssignableUserResponse> results = userRepository
                .findByOrgIdAndRoleIn(principal.getOrgId(), roles)
                .stream()
                .filter(User::isActive)
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .map(AssignableUserResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(
        summary = "List all therapists (and doctors) in the organisation",
        description = "Returns every THERAPIST and DOCTOR user in the caller's org. " +
                      "Pass an optional clinicId to scope the results to a single clinic. " +
                      "Accessible by BUSINESS_OWNER and ADMIN only."
    )
    @GetMapping("/therapists")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listTherapists(
            @RequestParam(required = false) UUID clinicId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!principal.getUser().hasRole(Role.BUSINESS_OWNER) &&
                !principal.getUser().hasRole(Role.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only BUSINESS_OWNER or ADMIN may list therapists");
        }

        List<UserResponse> results = (clinicId != null
                ? userRepository.findByOrgIdAndClinicIdAndRoleIn(principal.getOrgId(), clinicId, CLINICAL_ROLES)
                : userRepository.findByOrgIdAndRoleIn(principal.getOrgId(), CLINICAL_ROLES))
                .stream()
                .map(UserResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(
        summary = "Search users by email within this organisation",
        description = "Returns users whose email contains the query string. " +
                      "Optionally filter to users who hold a specific role (primary or additional). " +
                      "Minimum 2 characters required to avoid full-table scans."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> search(
            @RequestParam String email,
            @RequestParam(required = false) String role,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (email == null || email.trim().length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Search term must be at least 2 characters");
        }

        Role roleFilter = null;
        if (role != null) {
            try { roleFilter = Role.valueOf(role.toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown role: " + role);
            }
        }

        final Role finalRoleFilter = roleFilter;
        List<UserResponse> results = userRepository
                .findByOrgIdAndEmailContainingIgnoreCase(principal.getOrgId(), email.trim())
                .stream()
                .filter(u -> finalRoleFilter == null || u.hasRole(finalRoleFilter))
                .map(UserResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Deactivate a member — revokes login access while preserving audit records")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (id.equals(principal.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!target.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "User does not belong to this organisation");
        }
        target.setActive(false);
        userRepository.save(target);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Get my profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(principal.getUser())));
    }

    @Operation(
        summary = "Add an additional role to my account",
        description = "THERAPIST and BUSINESS_OWNER users may add PARENT as a secondary role."
    )
    @PostMapping("/me/roles")
    public ResponseEntity<ApiResponse<UserResponse>> addRole(
            @RequestBody AddRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Role roleToAdd = parseRole(request.role());

        if (!GRANTABLE_ADDITIONAL_ROLES.contains(roleToAdd)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Role '" + roleToAdd + "' cannot be added as an additional role");
        }

        User user = principal.getUser();
        if (!ELIGIBLE_PRIMARY_ROLES.contains(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only BUSINESS_OWNER, THERAPIST, or DOCTOR users may add a secondary role");
        }

        if (user.getAdditionalRoles().contains(roleToAdd)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You already have the " + roleToAdd + " role");
        }

        user.getAdditionalRoles().add(roleToAdd);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }

    @Operation(summary = "Remove an additional role from my account")
    @DeleteMapping("/me/roles/{role}")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
            @PathVariable String role,
            @AuthenticationPrincipal UserPrincipal principal) {

        Role roleToRemove = parseRole(role);

        if (roleToRemove == principal.getUser().getRole()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot remove your primary role");
        }

        User user = principal.getUser();
        if (!user.getAdditionalRoles().remove(roleToRemove)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "You do not have the " + roleToRemove + " role");
        }

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown role: " + value);
        }
    }

    public record AddRoleRequest(String role) {}
}

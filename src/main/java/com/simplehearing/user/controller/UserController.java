package com.simplehearing.user.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.user.dto.UserResponse;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Users", description = "User profile and role management")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /** Roles that can be added as additional (secondary) roles. */
    private static final Set<Role> GRANTABLE_ADDITIONAL_ROLES = Set.of(Role.PARENT);

    /** Primary roles allowed to acquire an additional role. */
    private static final Set<Role> ELIGIBLE_PRIMARY_ROLES = Set.of(Role.BUSINESS_OWNER, Role.THERAPIST, Role.DOCTOR);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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

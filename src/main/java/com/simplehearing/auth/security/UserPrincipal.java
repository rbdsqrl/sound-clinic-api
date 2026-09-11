package com.simplehearing.auth.security;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final User user;
    /** Which "hat" the caller is currently wearing in the UI (the frontend's role switcher),
     *  sent as the X-Active-Role header — defaults to the user's primary role when absent or
     *  not one the user actually holds. Endpoints where a dual-role person (e.g. a Therapist
     *  who is also a Parent) sees genuinely different data depending on which role they're
     *  viewing as (their calendar, their session list) should branch on this, not on
     *  getUser().getRole()/hasRole(), which reflect everything the account can do, not what
     *  it's currently asking to see. */
    private final Role activeRole;

    public UserPrincipal(User user) {
        this(user, user.getRole());
    }

    public UserPrincipal(User user, Role activeRole) {
        this.user = user;
        this.activeRole = (activeRole != null && user.hasRole(activeRole)) ? activeRole : user.getRole();
    }

    public User getUser() {
        return user;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public UUID getId() {
        return user.getId();
    }

    public UUID getOrgId() {
        return user.getOrgId();
    }

    public UUID getClinicId() {
        return user.getClinicId();
    }

    /** Every role the account holds (primary + additional) grants its authority — a
     *  @PreAuthorize check should reflect everything the account can do, per the class
     *  comment above. Gate on {@link #getActiveRole()} instead when an endpoint needs to
     *  branch on which hat the caller is currently viewing as, not on whether they're allowed
     *  in at all. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAllRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}

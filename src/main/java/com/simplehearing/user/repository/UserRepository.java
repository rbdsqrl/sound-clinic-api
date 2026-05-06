package com.simplehearing.user.repository;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByClinicIdAndRole(UUID clinicId, Role role);

    List<User> findByClinicId(UUID clinicId);

    List<User> findByOrgIdAndEmailContainingIgnoreCase(UUID orgId, String email);

    /** All users in an org whose primary role is one of the given roles. */
    List<User> findByOrgIdAndRoleIn(UUID orgId, Collection<Role> roles);

    /** Same as above, scoped to a single clinic. */
    List<User> findByOrgIdAndClinicIdAndRoleIn(UUID orgId, UUID clinicId, Collection<Role> roles);
}

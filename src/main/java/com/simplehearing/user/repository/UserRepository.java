package com.simplehearing.user.repository;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /*
     * Email is an identity, so it is matched case-insensitively. lower() rather than Spring Data's
     * IgnoreCase keyword (which emits upper()) so these hit uq_users_email_lower.
     */

    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE lower(u.email) = lower(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE lower(u.email) = lower(:email) AND u.isActive = :isActive")
    boolean existsByEmailAndIsActive(@Param("email") String email, @Param("isActive") boolean isActive);

    List<User> findByClinicIdAndRole(UUID clinicId, Role role);

    List<User> findByClinicId(UUID clinicId);

    List<User> findByOrgIdAndEmailContainingIgnoreCase(UUID orgId, String email);

    /** Matches the query against first name, last name, "First Last", or email. */
    @Query("SELECT u FROM User u WHERE u.orgId = :orgId AND (" +
           "lower(u.email) LIKE lower(concat('%', :q, '%')) OR " +
           "lower(u.firstName) LIKE lower(concat('%', :q, '%')) OR " +
           "lower(u.lastName) LIKE lower(concat('%', :q, '%')) OR " +
           "lower(concat(u.firstName, ' ', u.lastName)) LIKE lower(concat('%', :q, '%')))")
    List<User> searchByOrgId(@Param("orgId") UUID orgId, @Param("q") String q);

    /** All users in an org whose primary role is one of the given roles. */
    List<User> findByOrgIdAndRoleIn(UUID orgId, Collection<Role> roles);

    /** Same as above, scoped to a single clinic. */
    List<User> findByOrgIdAndClinicIdAndRoleIn(UUID orgId, UUID clinicId, Collection<Role> roles);
}

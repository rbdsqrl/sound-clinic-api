package com.simplehearing.invitation.repository;

import com.simplehearing.invitation.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    /** Email is matched case-insensitively — see UserRepository. */
    @Query("SELECT COUNT(i) > 0 FROM Invitation i "
         + "WHERE lower(i.email) = lower(:email) AND i.orgId = :orgId AND i.status = :status")
    boolean existsByEmailAndOrgIdAndStatus(@Param("email") String email,
                                           @Param("orgId") UUID orgId,
                                           @Param("status") Invitation.Status status);

    List<Invitation> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<Invitation> findByPatientId(UUID patientId);
}

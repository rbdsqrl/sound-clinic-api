package com.simplehearing.invitation.repository;

import com.simplehearing.invitation.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    boolean existsByEmailAndOrgIdAndStatus(String email, UUID orgId, Invitation.Status status);

    List<Invitation> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}

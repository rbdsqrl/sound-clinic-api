package com.simplehearing.memberdocument.repository;

import com.simplehearing.memberdocument.entity.MemberDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberDocumentRepository extends JpaRepository<MemberDocument, UUID> {

    List<MemberDocument> findByOrgIdAndMemberIdOrderByCreatedAtDesc(UUID orgId, UUID memberId);

    Optional<MemberDocument> findByIdAndOrgIdAndMemberId(UUID id, UUID orgId, UUID memberId);
}

package com.simplehearing.session.repository;

import com.simplehearing.session.entity.SessionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionAttachmentRepository extends JpaRepository<SessionAttachment, UUID> {
    List<SessionAttachment> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
}

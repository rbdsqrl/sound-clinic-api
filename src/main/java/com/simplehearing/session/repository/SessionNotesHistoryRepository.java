package com.simplehearing.session.repository;

import com.simplehearing.session.entity.SessionNotesHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionNotesHistoryRepository extends JpaRepository<SessionNotesHistory, UUID> {
    List<SessionNotesHistory> findBySessionIdOrderByChangedAtDesc(UUID sessionId);
}

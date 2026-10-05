package com.simplehearing.session.repository;

import com.simplehearing.session.entity.SessionActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionActivityEventRepository extends JpaRepository<SessionActivityEvent, UUID> {

    List<SessionActivityEvent> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}

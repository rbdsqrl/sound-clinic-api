package com.simplehearing.session.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.session.dto.SessionActivityResponse.Change;
import com.simplehearing.session.entity.SessionActivityEvent;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.SessionActivityType;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.SessionActivityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Writes the entries shown in a session's Activity Log. Recording is best-effort: a failure to
 * write the log must never fail the action it describes, so errors are logged and swallowed.
 */
@Service
public class SessionActivityService {

    private static final Logger log = LoggerFactory.getLogger(SessionActivityService.class);
    private static final int MAX_VALUE_CHARS = 1500;
    private static final TypeReference<List<Change>> CHANGES_TYPE = new TypeReference<>() {};

    // Plain mapper on purpose: the stored JSON shape must not drift with the API's Jackson settings.
    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionActivityEventRepository repository;

    public SessionActivityService(SessionActivityEventRepository repository) {
        this.repository = repository;
    }

    /** {@code actorId} null = the system. Does nothing for a non-persisted session. */
    public void record(TherapySession session, UUID actorId, SessionActivityType type,
                       String summary, List<Change> changes) {
        try {
            SessionActivityEvent event = new SessionActivityEvent();
            event.setOrgId(session.getOrgId());
            event.setSessionId(session.getId());
            event.setActorId(actorId);
            event.setEventType(type);
            event.setSummary(summary);
            event.setChanges(changes == null || changes.isEmpty() ? null : mapper.writeValueAsString(changes));
            repository.save(event);
        } catch (Exception e) {
            log.warn("Could not record session activity {} for session {}: {}", type, session.getId(), e.getMessage());
        }
    }

    public List<Change> parseChanges(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, CHANGES_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    // ── Helpers for building changes ─────────────────────────────────────────

    /** Adds a before/after entry only when the value actually changed. */
    public static void addIfChanged(List<Change> out, String field, String from, String to) {
        if (!Objects.equals(blankToNull(from), blankToNull(to))) {
            out.add(new Change(field, clip(from), clip(to)));
        }
    }

    public static Change change(String field, String from, String to) {
        return new Change(field, clip(from), clip(to));
    }

    public static List<Change> list() {
        return new ArrayList<>();
    }

    /** "PENDING_RESCHEDULE" -> "Pending reschedule". */
    public static String statusLabel(TherapySessionStatus status) {
        if (status == null) return null;
        String s = status.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static String clip(String v) {
        if (v == null || v.isBlank()) return null;
        return v.length() > MAX_VALUE_CHARS ? v.substring(0, MAX_VALUE_CHARS) + "…" : v;
    }
}

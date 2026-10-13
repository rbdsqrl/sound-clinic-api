package com.simplehearing.auditlog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.auditlog.entity.AuditLogEntry;
import com.simplehearing.auditlog.repository.AuditLogEntryRepository;
import com.simplehearing.session.dto.SessionActivityResponse.Change;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Writes the entries shown in the Activity Log tab of a member, parent or case. Recording is
 * best-effort: a failure to write the log must never fail the action it describes, so errors
 * are logged and swallowed.
 */
@Service
public class AuditLogService {

    public static final String USER = "USER";
    public static final String CASE = "CASE";

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_VALUE_CHARS = 500;
    private static final TypeReference<List<Change>> CHANGES_TYPE = new TypeReference<>() {};

    // Plain mapper on purpose: the stored JSON shape must not drift with the API's Jackson settings.
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuditLogEntryRepository repository;

    public AuditLogService(AuditLogEntryRepository repository) {
        this.repository = repository;
    }

    /** {@code actorId} null = the system. */
    public void record(UUID orgId, String subjectType, UUID subjectId, UUID actorId,
                       String eventType, String summary, List<Change> changes) {
        try {
            AuditLogEntry entry = new AuditLogEntry();
            entry.setOrgId(orgId);
            entry.setSubjectType(subjectType);
            entry.setSubjectId(subjectId);
            entry.setActorId(actorId);
            entry.setEventType(eventType);
            entry.setSummary(summary.length() > 255 ? summary.substring(0, 252) + "..." : summary);
            entry.setChanges(changes == null || changes.isEmpty() ? null : mapper.writeValueAsString(changes));
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Could not record audit log {} for {} {}: {}", eventType, subjectType, subjectId, e.getMessage());
        }
    }

    public void recordUser(UUID orgId, UUID userId, UUID actorId, String eventType, String summary, List<Change> changes) {
        record(orgId, USER, userId, actorId, eventType, summary, changes);
    }

    public void recordCase(UUID orgId, UUID patientId, UUID actorId, String eventType, String summary, List<Change> changes) {
        record(orgId, CASE, patientId, actorId, eventType, summary, changes);
    }

    public List<Change> parseChanges(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, CHANGES_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    // ── helpers for building a change list ───────────────────────────────────

    public static List<Change> list() {
        return new ArrayList<>();
    }

    public static Change change(String field, String from, String to) {
        return new Change(field, clip(from), clip(to));
    }

    /** Adds {field: from → to} only when the two differ (null and blank count as the same "not set"). */
    public static void addIfChanged(List<Change> changes, String field, Object from, Object to) {
        String f = normalise(from);
        String t = normalise(to);
        if (!Objects.equals(f, t)) changes.add(change(field, f, t));
    }

    private static String normalise(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String clip(String v) {
        if (v == null) return null;
        return v.length() > MAX_VALUE_CHARS ? v.substring(0, MAX_VALUE_CHARS) + "…" : v;
    }
}

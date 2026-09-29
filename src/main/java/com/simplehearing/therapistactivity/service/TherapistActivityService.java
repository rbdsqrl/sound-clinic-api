package com.simplehearing.therapistactivity.service;

import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.sharedmedia.entity.SharedMedia;
import com.simplehearing.sharedmedia.repository.SharedMediaRepository;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse.ChildFreeTextNotes;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse.ChildMedia;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse.ChildSessionNotes;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse.FreeTextNoteEntry;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse.MediaEntry;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse.SessionNoteEntry;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TherapistActivityService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final TherapySessionRepository therapySessionRepository;
    private final SharedMediaRepository sharedMediaRepository;

    public TherapistActivityService(UserRepository userRepository,
                                     PatientRepository patientRepository,
                                     TherapySessionRepository therapySessionRepository,
                                     SharedMediaRepository sharedMediaRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.therapySessionRepository = therapySessionRepository;
        this.sharedMediaRepository = sharedMediaRepository;
    }

    public TherapistActivityResponse getActivity(UUID orgId, UUID therapistId, LocalDate date) {
        User therapist = userRepository.findById(therapistId)
                .filter(u -> u.getOrgId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
        String therapistName = therapist.getFirstName() + " " + therapist.getLastName();

        List<TherapySession> notedSessions = therapySessionRepository
                .findByOrgIdAndTherapistIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                        orgId, therapistId, date, date)
                .stream()
                .filter(s -> hasText(s.getNotes()) || hasText(s.getProgressReport()) || hasText(s.getFeedback()))
                .toList();

        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<SharedMedia> uploads = sharedMediaRepository
                .findByOrgIdAndUploadedByAndCreatedAtBetween(orgId, therapistId, dayStart, dayEnd);
        List<SharedMedia> freeTextEntries = uploads.stream().filter(m -> hasText(m.getNote())).toList();
        List<SharedMedia> mediaEntries = uploads.stream().filter(m -> hasText(m.getFileUrl())).toList();

        Set<UUID> patientIds = new HashSet<>();
        notedSessions.forEach(s -> patientIds.add(s.getPatientId()));
        freeTextEntries.forEach(m -> patientIds.add(m.getPatientId()));
        mediaEntries.forEach(m -> patientIds.add(m.getPatientId()));
        Map<UUID, String> patientNames = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p.getFirstName() + " " + p.getLastName()));

        List<ChildSessionNotes> sessionGroups = notedSessions.stream()
                .collect(Collectors.groupingBy(TherapySession::getPatientId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new ChildSessionNotes(
                        e.getKey(),
                        patientNames.getOrDefault(e.getKey(), ""),
                        e.getValue().stream().map(s -> new SessionNoteEntry(
                                s.getId(), s.getStartTime(), s.getNotes(), s.getProgressReport(),
                                s.getFeedback(), s.getPerformanceScore())).toList()))
                .toList();

        List<ChildFreeTextNotes> freeTextGroups = freeTextEntries.stream()
                .collect(Collectors.groupingBy(SharedMedia::getPatientId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new ChildFreeTextNotes(
                        e.getKey(),
                        patientNames.getOrDefault(e.getKey(), ""),
                        e.getValue().stream().map(m -> new FreeTextNoteEntry(
                                m.getId(), m.getCreatedAt(), m.getNote())).toList()))
                .toList();

        List<ChildMedia> mediaGroups = mediaEntries.stream()
                .collect(Collectors.groupingBy(SharedMedia::getPatientId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new ChildMedia(
                        e.getKey(),
                        patientNames.getOrDefault(e.getKey(), ""),
                        e.getValue().stream().map(m -> new MediaEntry(
                                m.getId(), m.getCreatedAt(), m.getFileUrl(), m.getFileName(),
                                m.getContentType(), m.getNote())).toList()))
                .toList();

        return new TherapistActivityResponse(
                therapistId, therapistName, date,
                notedSessions.size(), sessionGroups,
                freeTextEntries.size(), freeTextGroups,
                mediaEntries.size(), mediaGroups
        );
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

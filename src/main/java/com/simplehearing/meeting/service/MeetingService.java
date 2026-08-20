package com.simplehearing.meeting.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.dto.ParticipantResponse;
import com.simplehearing.meeting.dto.*;
import com.simplehearing.meeting.entity.Meeting;
import com.simplehearing.meeting.enums.MeetingStatus;
import com.simplehearing.meeting.repository.MeetingRepository;
import com.simplehearing.notification.CalendarInviteService;
import com.simplehearing.notification.EmailProperties;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final CalendarInviteService calendarInviteService;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public MeetingService(MeetingRepository meetingRepository,
                          UserRepository userRepository,
                          OrganisationRepository organisationRepository,
                          CalendarInviteService calendarInviteService,
                          EmailService emailService,
                          EmailProperties emailProperties) {
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.calendarInviteService = calendarInviteService;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @Transactional
    public MeetingResponse create(CreateMeetingRequest request, UUID orgId, UUID createdBy) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after the start time");
        }

        // The organiser always attends; everyone else must belong to the same organisation.
        Set<UUID> ids = new LinkedHashSet<>(request.participantIds());
        ids.add(createdBy);

        List<User> participants = userRepository.findAllById(ids);
        if (participants.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more participants could not be found");
        }
        boolean foreign = participants.stream().anyMatch(u -> !orgId.equals(u.getOrgId()));
        if (foreign) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Participants must belong to your organisation");
        }

        Meeting meeting = new Meeting();
        meeting.setOrgId(orgId);
        meeting.setTitle(request.title().trim());
        meeting.setDescription(request.description());
        meeting.setMeetingDate(request.meetingDate());
        meeting.setStartTime(request.startTime());
        meeting.setEndTime(request.endTime());
        meeting.setLocation(request.location());
        meeting.setCreatedBy(createdBy);
        meeting.setParticipantIds(ids);
        meeting.setIcsUid(UUID.randomUUID() + "@simplehearing");

        Meeting saved = meetingRepository.save(meeting);
        sendInvites(saved, participants, false);
        return toResponse(saved, participants);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> list(UUID orgId, UUID userId, boolean seesEverything,
                                      LocalDate from, LocalDate to) {
        List<Meeting> meetings = seesEverything
                ? meetingRepository.findByOrgIdAndMeetingDateBetweenOrderByMeetingDateAscStartTimeAsc(orgId, from, to)
                : meetingRepository.findVisibleTo(orgId, userId, from, to);

        // One lookup for every participant across the window rather than per meeting.
        Set<UUID> allIds = meetings.stream()
                .flatMap(m -> m.getParticipantIds().stream())
                .collect(Collectors.toSet());
        Map<UUID, User> byId = userRepository.findAllById(allIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return meetings.stream()
                .map(m -> toResponse(m, m.getParticipantIds().stream()
                        .map(byId::get).filter(Objects::nonNull).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(UUID id, UUID orgId) {
        Meeting m = require(id, orgId);
        return toResponse(m, userRepository.findAllById(m.getParticipantIds()));
    }

    @Transactional
    public MeetingResponse cancel(UUID id, UUID orgId, UUID actorId, String reason) {
        Meeting m = require(id, orgId);
        if (m.getStatus() == MeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting is already cancelled");
        }
        m.setStatus(MeetingStatus.CANCELLED);
        m.setCancelledReason(reason);
        m.setIcsSequence(m.getIcsSequence() + 1);
        Meeting saved = meetingRepository.save(m);

        List<User> participants = userRepository.findAllById(saved.getParticipantIds());
        sendCancellations(saved, participants, reason);
        return toResponse(saved, participants);
    }

    private Meeting require(UUID id, UUID orgId) {
        Meeting m = meetingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));
        if (!orgId.equals(m.getOrgId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Meeting not found");
        }
        return m;
    }

    private MeetingResponse toResponse(Meeting m, List<User> participants) {
        String organiserName = participants.stream()
                .filter(u -> u.getId().equals(m.getCreatedBy()))
                .findFirst()
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        List<ParticipantResponse> people = participants.stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .map(u -> ParticipantResponse.from(u, u.getId().equals(m.getCreatedBy())))
                .toList();

        return MeetingResponse.from(m, organiserName, people);
    }

    // ── Notifications ───────────────────────────────────────────────────────────

    private void sendInvites(Meeting m, List<User> participants, boolean rescheduled) {
        String orgName = orgName(m.getOrgId());
        String organiser = participants.stream()
                .filter(u -> u.getId().equals(m.getCreatedBy()))
                .findFirst().map(this::fullName).orElse(orgName);
        String names = participants.stream().map(this::fullName).collect(Collectors.joining(", "));

        String ics = calendarInviteService.build(
                m.getIcsUid(), m.getIcsSequence(), CalendarInviteService.Method.REQUEST,
                m.getTitle(),
                m.getDescription() != null ? m.getDescription() : "Meeting organised by " + organiser + ".",
                m.getLocation(), m.getMeetingDate(), m.getStartTime(), m.getEndTime(),
                orgName, emailProperties.getFromAddress(),
                participants.stream()
                        .map(u -> new CalendarInviteService.Attendee(fullName(u), u.getEmail()))
                        .toList());

        String dateLabel = m.getMeetingDate().format(DATE_LABEL);
        String timeLabel = m.getStartTime().format(TIME_LABEL);

        for (User u : participants) {
            emailService.sendMeetingInvite(u.getEmail(), u.getFirstName(), m.getTitle(),
                    organiser, names, m.getLocation(), dateLabel, timeLabel, orgName,
                    "/calendar?meeting=" + m.getId(), ics, rescheduled);
        }
        log.info("Meeting {} invites queued for {} participant(s)", m.getId(), participants.size());
    }

    private void sendCancellations(Meeting m, List<User> participants, String reason) {
        String orgName = orgName(m.getOrgId());
        String ics = calendarInviteService.build(
                m.getIcsUid(), m.getIcsSequence(), CalendarInviteService.Method.CANCEL,
                m.getTitle(), "This meeting has been cancelled.",
                m.getLocation(), m.getMeetingDate(), m.getStartTime(), m.getEndTime(),
                orgName, emailProperties.getFromAddress(),
                participants.stream()
                        .map(u -> new CalendarInviteService.Attendee(fullName(u), u.getEmail()))
                        .toList());

        String dateLabel = m.getMeetingDate().format(DATE_LABEL);
        for (User u : participants) {
            emailService.sendMeetingCancelled(u.getEmail(), u.getFirstName(), m.getTitle(),
                    dateLabel, orgName, reason, ics);
        }
    }

    private String fullName(User u) { return u.getFirstName() + " " + u.getLastName(); }

    private String orgName(UUID orgId) {
        return organisationRepository.findById(orgId)
                .map(o -> o.getName())
                .orElse("Simple Hearing");
    }
}

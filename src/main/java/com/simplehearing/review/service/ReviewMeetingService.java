package com.simplehearing.review.service;

import java.util.Comparator;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.notification.CalendarInviteService;
import com.simplehearing.notification.EmailProperties;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.review.dto.ReviewScheduleRequest;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.enums.ReviewMeetingStatus;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates review meetings for an enrollment and keeps everyone's calendar in step.
 */
@Service
public class ReviewMeetingService {

    private static final Logger log = LoggerFactory.getLogger(ReviewMeetingService.class);

    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("h:mm a");

    private final ReviewMeetingRepository meetingRepository;
    private final PublicHolidayRepository holidayRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final ClinicRepository clinicRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final CalendarInviteService calendarInviteService;

    public ReviewMeetingService(ReviewMeetingRepository meetingRepository,
                                PublicHolidayRepository holidayRepository,
                                PatientRepository patientRepository,
                                PatientParentRepository patientParentRepository,
                                UserRepository userRepository,
                                OrganisationRepository organisationRepository,
                                ClinicRepository clinicRepository,
                                EmailService emailService,
                                EmailProperties emailProperties,
                                CalendarInviteService calendarInviteService) {
        this.meetingRepository = meetingRepository;
        this.holidayRepository = holidayRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.clinicRepository = clinicRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.calendarInviteService = calendarInviteService;
    }

    // ── Generation ───────────────────────────────────────────────────────────

    /**
     * Creates the review meetings for a freshly-set-up therapy plan, spaced by the
     * requested interval and skipping public holidays the way therapy sessions do.
     *
     * @return the meetings created, in date order
     */
    @Transactional
    public List<ReviewMeeting> generateForEnrollment(Enrollment enrollment,
                                                     ReviewScheduleRequest schedule,
                                                     UUID createdBy) {

        int intervalWeeks = schedule.intervalWeeksOrDefault();
        int durationMinutes = schedule.durationMinutesOrDefault();

        LocalDate windowEnd = schedule.endDate() != null ? schedule.endDate() : enrollment.getEndDate();
        if (windowEnd == null) {
            log.warn("No end date for enrollment {} — cannot generate review meetings", enrollment.getId());
            return List.of();
        }

        LocalDate first = schedule.firstMeetingDate() != null
                ? schedule.firstMeetingDate()
                : enrollment.getStartDate().plusWeeks(intervalWeeks);

        if (first.isAfter(windowEnd)) {
            log.info("First review meeting {} falls past the end of the plan {} — none generated", first, windowEnd);
            return List.of();
        }

        Set<LocalDate> holidays = holidayRepository
                .findByOrgIdOrderByHolidayDateAsc(enrollment.getOrgId())
                .stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());

        LocalTime startTime = schedule.startTime();
        LocalTime endTime = startTime.plusMinutes(durationMinutes);

        List<ReviewMeeting> meetings = new ArrayList<>();
        LocalDate date = first;
        int number = 1;

        while (!date.isAfter(windowEnd)) {
            LocalDate slot = date;
            // Nudge past holidays, but never past the end of the plan.
            while (holidays.contains(slot) && !slot.isAfter(windowEnd)) {
                slot = slot.plusDays(1);
            }
            if (slot.isAfter(windowEnd)) break;

            ReviewMeeting m = new ReviewMeeting();
            m.setOrgId(enrollment.getOrgId());
            m.setEnrollmentId(enrollment.getId());
            m.setPatientId(enrollment.getPatientId());
            m.setTherapistId(enrollment.getTherapistId());
            m.setMeetingNumber(number++);
            m.setMeetingDate(slot);
            m.setStartTime(startTime);
            m.setEndTime(endTime);
            m.setIcsUid(newIcsUid());
            m.setCreatedBy(createdBy);
            meetings.add(m);

            date = date.plusWeeks(intervalWeeks);
        }

        List<ReviewMeeting> saved = meetingRepository.saveAll(meetings);
        log.info("Generated {} review meetings for enrollment {}", saved.size(), enrollment.getId());

        saved.forEach(m -> sendInvites(m, false));
        return saved;
    }

    /** Creates one extra meeting outside the generated rhythm. */
    @Transactional
    public ReviewMeeting createSingle(Enrollment enrollment, LocalDate date, LocalTime startTime,
                                      int durationMinutes, UUID createdBy) {
        ReviewMeeting m = new ReviewMeeting();
        m.setOrgId(enrollment.getOrgId());
        m.setEnrollmentId(enrollment.getId());
        m.setPatientId(enrollment.getPatientId());
        m.setTherapistId(enrollment.getTherapistId());
        m.setMeetingDate(date);
        m.setStartTime(startTime);
        m.setEndTime(startTime.plusMinutes(durationMinutes));
        m.setIcsUid(newIcsUid());
        m.setCreatedBy(createdBy);

        ReviewMeeting saved = meetingRepository.save(m);

        // A meeting booked for an earlier date than existing ones would otherwise take the
        // highest number, so "Review 3" could fall before "Review 1". Number the whole plan
        // by date instead, which also corrects any sequence already out of order.
        renumberByDate(enrollment.getId());
        ReviewMeeting renumbered = meetingRepository.findById(saved.getId()).orElse(saved);

        sendInvites(renumbered, false);
        return renumbered;
    }

    /**
     * Rewrites meetingNumber across a plan in date order. Cancelled meetings keep their
     * place in the sequence so the numbers people have already been emailed still line up.
     */
    @Transactional
    public void renumberByDate(UUID enrollmentId) {
        List<ReviewMeeting> all = meetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(enrollmentId)
                .stream()
                .sorted(Comparator.comparing(ReviewMeeting::getMeetingDate)
                        .thenComparing(ReviewMeeting::getStartTime))
                .toList();

        int n = 1;
        for (ReviewMeeting m : all) {
            if (m.getMeetingNumber() != n) {
                m.setMeetingNumber(n);
                meetingRepository.save(m);
            }
            n++;
        }
    }

    // ── Calendar notifications ───────────────────────────────────────────────

    /** Emails the therapist and every linked parent an invite carrying an .ics attachment. */
    public void sendInvites(ReviewMeeting meeting, boolean rescheduled) {
        Context ctx = contextFor(meeting);
        if (ctx == null) return;

        String ics = calendarInviteService.build(
                meeting.getIcsUid(),
                meeting.getIcsSequence(),
                CalendarInviteService.Method.REQUEST,
                "Review meeting — " + ctx.patientName(),
                "Progress review for " + ctx.patientName() + " with " + ctx.therapistName() + ".",
                ctx.location(),
                meeting.getMeetingDate(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                ctx.orgName(),
                emailProperties.getFromAddress(),
                ctx.attendees());

        String dateLabel = meeting.getMeetingDate().format(DATE_LABEL);
        String timeLabel = meeting.getStartTime().format(TIME_LABEL);
        String url = "/patients/" + meeting.getPatientId() + "?review=" + meeting.getId();

        for (Recipient r : ctx.recipients()) {
            emailService.sendReviewMeetingInvite(
                    r.email(), r.name(), ctx.patientName(), ctx.therapistName(),
                    dateLabel, timeLabel, ctx.orgName(), url, ics, rescheduled);
        }
    }

    /** Emails a CANCEL-method invite so the entry drops out of everyone's calendar. */
    public void sendCancellations(ReviewMeeting meeting, String reason) {
        Context ctx = contextFor(meeting);
        if (ctx == null) return;

        String ics = calendarInviteService.build(
                meeting.getIcsUid(),
                meeting.getIcsSequence(),
                CalendarInviteService.Method.CANCEL,
                "Review meeting — " + ctx.patientName(),
                "This review meeting has been cancelled.",
                ctx.location(),
                meeting.getMeetingDate(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                ctx.orgName(),
                emailProperties.getFromAddress(),
                ctx.attendees());

        String dateLabel = meeting.getMeetingDate().format(DATE_LABEL);

        for (Recipient r : ctx.recipients()) {
            emailService.sendReviewMeetingCancelled(
                    r.email(), r.name(), ctx.patientName(), dateLabel, ctx.orgName(), reason, ics);
        }
    }

    /** Notifies the other side that feedback has landed. */
    public void notifyFeedbackSubmitted(ReviewMeeting meeting, boolean fromParent) {
        Context ctx = contextFor(meeting);
        if (ctx == null) return;

        String dateLabel = meeting.getMeetingDate().format(DATE_LABEL);
        String url = "/patients/" + meeting.getPatientId() + "?review=" + meeting.getId();

        List<Recipient> targets = fromParent
                ? ctx.recipients().stream().filter(r -> r.isTherapist).toList()
                : ctx.recipients().stream().filter(r -> !r.isTherapist).toList();

        for (Recipient r : targets) {
            emailService.sendReviewFeedbackNotification(
                    r.email(), r.name(), ctx.patientName(), dateLabel, ctx.orgName(), url);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** A UID that stays stable for this meeting across reschedules. */
    private static String newIcsUid() {
        return UUID.randomUUID() + "@simplehearing.in";
    }

    /** Everyone and everything needed to build an invite. Null when the patient has vanished. */
    private Context contextFor(ReviewMeeting meeting) {
        Optional<Patient> patient = patientRepository.findById(meeting.getPatientId());
        if (patient.isEmpty()) {
            log.warn("Review meeting {} has no patient — skipping notification", meeting.getId());
            return null;
        }

        String patientName = patient.get().getFirstName() + " " + patient.get().getLastName();

        User therapist = userRepository.findById(meeting.getTherapistId()).orElse(null);
        String therapistName = therapist != null
                ? therapist.getFirstName() + " " + therapist.getLastName()
                : "the therapist";

        String orgName = organisationRepository.findById(meeting.getOrgId())
                .map(Organisation::getName)
                .orElse("Simple Hearing");

        String location = therapist != null && therapist.getClinicId() != null
                ? clinicRepository.findById(therapist.getClinicId()).map(c -> c.getName()).orElse("")
                : "";

        List<Recipient> recipients = new ArrayList<>();
        if (therapist != null && therapist.getEmail() != null && therapist.isActive()) {
            recipients.add(new Recipient(therapistName, therapist.getEmail(), true));
        }

        List<UUID> parentIds = patientParentRepository.findById_PatientId(meeting.getPatientId())
                .stream()
                .map(pp -> pp.getId().getParentId())
                .toList();

        userRepository.findAllById(parentIds).stream()
                .filter(User::isActive)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .forEach(u -> recipients.add(
                        new Recipient(u.getFirstName() + " " + u.getLastName(), u.getEmail(), false)));

        List<CalendarInviteService.Attendee> attendees = recipients.stream()
                .map(r -> new CalendarInviteService.Attendee(r.name(), r.email()))
                .toList();

        return new Context(patientName, therapistName, orgName, location, recipients, attendees);
    }

    private record Recipient(String name, String email, boolean isTherapist) {}

    private record Context(String patientName,
                           String therapistName,
                           String orgName,
                           String location,
                           List<Recipient> recipients,
                           List<CalendarInviteService.Attendee> attendees) {}

    // ── Status transitions used by the controller ────────────────────────────

    @Transactional
    public ReviewMeeting reschedule(ReviewMeeting meeting, LocalDate date, LocalTime startTime,
                                    Integer durationMinutes) {
        int minutes = durationMinutes != null
                ? durationMinutes
                : (int) java.time.Duration.between(meeting.getStartTime(), meeting.getEndTime()).toMinutes();

        meeting.setMeetingDate(date);
        meeting.setStartTime(startTime);
        meeting.setEndTime(startTime.plusMinutes(minutes));
        meeting.setStatus(ReviewMeetingStatus.SCHEDULED);
        meeting.setIcsSequence(meeting.getIcsSequence() + 1);   // makes clients update, not duplicate

        ReviewMeeting saved = meetingRepository.save(meeting);

        // Moving a meeting can change where it falls in the sequence.
        renumberByDate(saved.getEnrollmentId());
        ReviewMeeting renumbered = meetingRepository.findById(saved.getId()).orElse(saved);

        sendInvites(renumbered, true);
        return renumbered;
    }

    @Transactional
    public ReviewMeeting cancel(ReviewMeeting meeting, String reason) {
        meeting.setStatus(ReviewMeetingStatus.CANCELLED);
        meeting.setCancelledReason(reason);
        meeting.setIcsSequence(meeting.getIcsSequence() + 1);

        ReviewMeeting saved = meetingRepository.save(meeting);
        sendCancellations(saved, reason);
        return saved;
    }
}

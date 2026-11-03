package com.simplehearing.report.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Emails every Admin Role user a session report twice a day: a day report at 11:40 AM (today's schedule so far)
 * and a day end report at 1:00 PM (how the previous day ended), fired by {@code SessionReportJob}'s two cron triggers.
 * Modelled on Kidaura's day-start / day-end session reports. "Today" is each organisation's own local date.
 */
@Service
public class SessionReportService {

    private static final Logger log = LoggerFactory.getLogger(SessionReportService.class);


    private static final Set<Role> ADMIN_ROLES = EnumSet.of(Role.CLINIC_HEAD, Role.OFFICE_ADMIN, Role.BUSINESS_OWNER);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private enum Kind { DAY, NIGHT }

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final TherapySessionRepository sessionRepository;
    private final PatientRepository patientRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final EmailService emailService;

    public SessionReportService(OrganisationRepository organisationRepository,
                                UserRepository userRepository,
                                TherapySessionRepository sessionRepository,
                                PatientRepository patientRepository,
                                EnrollmentRepository enrollmentRepository,
                                SubscriptionRepository subscriptionRepository,
                                ProgramRepository programRepository,
                                EmailService emailService) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.patientRepository = patientRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public void sendDayReport() {
        sendToAllOrganisations(Kind.DAY);
    }

    @Transactional(readOnly = true)
    public void sendNightReport() {
        sendToAllOrganisations(Kind.NIGHT);
    }

    private void sendToAllOrganisations(Kind kind) {
        for (Organisation org : organisationRepository.findAll()) {
            try {
                LocalDate today = LocalDate.now(zoneOf(org));
                send(org, kind, kind == Kind.NIGHT ? today.minusDays(1) : today);
            } catch (Exception e) {
                log.error("Session report ({}) failed for org {}: {}", kind, org.getId(), e.getMessage(), e);
            }
        }
    }

    private void send(Organisation org, Kind kind, LocalDate date) {
        List<String> recipients = userRepository.findByOrgId(org.getId()).stream()
                .filter(User::isActive)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .filter(u -> u.getAllRoles().stream().anyMatch(ADMIN_ROLES::contains))
                .map(User::getEmail)
                .distinct()
                .toList();
        if (recipients.isEmpty()) return;

        List<TherapySession> sessions = sessionRepository
                .findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(org.getId(), date, date);

        String title = kind == Kind.DAY ? "Day session report" : "Day end session report";
        String html = build(org, kind, date, sessions);
        emailService.sendHtmlReport(recipients, "[" + org.getName() + "] " + title + " - " + date.format(DATE_FMT), html);
        log.info("Queued {} for org {} to {} admin(s), {} session(s)", title, org.getId(), recipients.size(), sessions.size());
    }

    // ── Report building ─────────────────────────────────────────────────────────────────────────────────

    private String build(Organisation org, Kind kind, LocalDate date, List<TherapySession> sessions) {
        Map<UUID, String> patientNames = patientRepository
                .findAllById(sessions.stream().map(TherapySession::getPatientId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Patient::getId, p -> fullName(p.getFirstName(), p.getLastName())));
        Map<UUID, String> therapistNames = userRepository
                .findAllById(sessions.stream().map(TherapySession::getTherapistId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, u -> fullName(u.getFirstName(), u.getLastName())));
        Map<UUID, String> programNames = programNamesByEnrollment(sessions);

        long total = sessions.size();
        long cancelled = count(sessions, TherapySessionStatus.CANCELLED);
        long completed = count(sessions, TherapySessionStatus.COMPLETED);
        long noShow = count(sessions, TherapySessionStatus.NO_SHOW);
        long marked = completed + noShow;
        long pendingRequests = count(sessions, TherapySessionStatus.PENDING_RESCHEDULE)
                + count(sessions, TherapySessionStatus.CANCELLATION_REQUESTED);
        long notMarked = total - cancelled - marked;
        long rescheduled = sessions.stream().filter(s -> s.getRescheduleCount() > 0).count();
        long withNotes = sessions.stream().filter(SessionReportService::hasNotes).count();

        StringBuilder h = new StringBuilder(8192);
        h.append("<!DOCTYPE html><html lang=\"en\"><body style=\"margin:0;padding:24px 12px;background:#f4f5f7;")
                .append("font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;color:#1f2937;\">")
                .append("<div style=\"max-width:640px;margin:0 auto;\">")
                .append("<h1 style=\"text-align:center;font-size:22px;color:#1e3a8a;margin:0 0 20px;\">")
                .append(kind == Kind.DAY ? "Day Session Report of " : "Day End Session Report of ")
                .append(esc(org.getName())).append("</h1>");

        // Stats grid. The day report is a mid-morning snapshot, so outcome cells that can't be known yet show "--".
        boolean night = kind == Kind.NIGHT;
        h.append(card()).append("<h2 style=\"margin:0 0 12px;font-size:16px;color:#1e3a8a;\">Stats (")
                .append(date.format(DATE_FMT)).append(")</h2>")
                .append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">");
        statRow(h,
                stat(String.valueOf(total), "Total Sessions"),
                stat(night ? pct(marked, total - cancelled) : String.valueOf(marked), "Attendance Marked (" + marked + ")"),
                stat(night ? pct(notMarked, total - cancelled) : "--", "Not Marked" + (night ? " (" + notMarked + ")" : "")),
                stat(night ? pct(completed, total) : String.valueOf(completed), "Completed (" + completed + ")"));
        statRow(h,
                stat(String.valueOf(cancelled), "Cancelled"),
                stat(String.valueOf(noShow), "No Shows"),
                stat(String.valueOf(rescheduled), "Rescheduled"),
                stat(String.valueOf(pendingRequests), "Pending Requests"));
        if (night) {
            statRow(h,
                    stat(String.valueOf(withNotes), "Sessions With Notes"),
                    stat(String.valueOf(total - cancelled - withNotes), "Sessions Without Notes"),
                    stat("", ""), stat("", ""));
        }
        h.append("</table></div>");

        // Per-therapist grouping, busiest first.
        Map<UUID, List<TherapySession>> byTherapist = sessions.stream()
                .collect(Collectors.groupingBy(TherapySession::getTherapistId, LinkedHashMap::new, Collectors.toList()));
        List<Map.Entry<UUID, List<TherapySession>>> groups = byTherapist.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<UUID, List<TherapySession>> e) -> therapistNames.getOrDefault(e.getKey(), "")))
                .toList();

        if (night && !groups.isEmpty()) {
            h.append(card()).append("<h2 style=\"margin:0 0 12px;font-size:16px;color:#1e3a8a;\">Therapist Session Summary</h2>")
                    .append("<table width=\"100%\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;font-size:13px;\">")
                    .append("<tr style=\"background:#f3f4f6;\"><th align=\"left\" style=\"border:1px solid #d1d5db;\">Therapist</th>")
                    .append(th("Scheduled")).append(th("Cancelled")).append(th("Attended")).append(th("With Notes")).append("</tr>");
            for (var g : groups) {
                List<TherapySession> list = g.getValue();
                h.append("<tr><td style=\"border:1px solid #d1d5db;\">").append(esc(therapistNames.getOrDefault(g.getKey(), "Unknown")))
                        .append("</td>").append(td(list.size()))
                        .append(td(count(list, TherapySessionStatus.CANCELLED)))
                        .append(td(count(list, TherapySessionStatus.COMPLETED)))
                        .append(td(list.stream().filter(SessionReportService::hasNotes).count())).append("</tr>");
            }
            h.append("</table></div>");
        }

        if (groups.isEmpty()) {
            h.append(card()).append("<p style=\"margin:0;font-size:14px;color:#6b7280;\">No sessions are scheduled for today.</p></div>");
        }
        for (var g : groups) {
            List<TherapySession> list = g.getValue();
            h.append(card()).append("<h3 style=\"margin:0 0 4px;font-size:16px;\">")
                    .append(esc(therapistNames.getOrDefault(g.getKey(), "Unknown therapist"))).append("</h3>")
                    .append("<p style=\"margin:0 0 12px;font-size:12px;color:#6b7280;\">Total Sessions: ").append(list.size());
            if (night) {
                long gMarked = count(list, TherapySessionStatus.COMPLETED) + count(list, TherapySessionStatus.NO_SHOW);
                long gCancelled = count(list, TherapySessionStatus.CANCELLED);
                h.append(" &middot; Marked: ").append(gMarked)
                        .append(" &middot; Not Marked: ").append(list.size() - gCancelled - gMarked)
                        .append(" &middot; Cancelled: ").append(gCancelled);
            }
            h.append("</p><ol style=\"margin:0;padding-left:20px;font-size:14px;line-height:1.6;\">");
            for (TherapySession s : list) {
                h.append("<li>").append(timeRange(s.getStartTime(), s.getEndTime())).append(": ")
                        .append(esc(programNames.getOrDefault(s.getEnrollmentId(), "Therapy"))).append(" - ")
                        .append(esc(patientNames.getOrDefault(s.getPatientId(), "Unknown")));
                if (s.isAdHoc()) h.append(" <span style=\"color:#6b7280;\">(ad hoc)</span>");
                if (night || s.getStatus() != TherapySessionStatus.SCHEDULED) {
                    h.append(" <span style=\"color:").append(statusColor(s.getStatus())).append(";font-weight:600;\">[")
                            .append(statusLabel(s.getStatus(), night)).append("]</span>");
                }
                h.append("</li>");
            }
            h.append("</ol></div>");
        }

        h.append("<p style=\"font-size:12px;color:#6b7280;line-height:1.6;\">")
                .append("<b>Attendance Marked:</b> sessions marked completed or no show. ")
                .append("<b>Not Marked:</b> sessions still scheduled with no outcome recorded. ")
                .append("<b>Rescheduled:</b> this day's sessions that have been moved at least once. ")
                .append("<b>Pending Requests:</b> sessions awaiting a reschedule or cancellation decision. ")
                .append("<b>With Notes:</b> sessions with notes, feedback, a progress report or checklist notes saved.</p>")
                .append("<p style=\"font-size:12px;color:#9ca3af;text-align:center;\">&copy; ").append(esc(org.getName()))
                .append(". Sent to admins only.</p></div></body></html>");
        return h.toString();
    }

    private Map<UUID, String> programNamesByEnrollment(List<TherapySession> sessions) {
        Map<UUID, Enrollment> enrollments = enrollmentRepository
                .findAllById(sessions.stream().map(TherapySession::getEnrollmentId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Enrollment::getId, Function.identity()));
        Map<UUID, Subscription> subs = subscriptionRepository
                .findAllById(enrollments.values().stream().map(Enrollment::getSubscriptionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subscription::getId, Function.identity()));
        Map<UUID, String> programs = programRepository
                .findAllById(subs.values().stream().map(Subscription::getProgramId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Program::getId, Program::getName));
        Map<UUID, String> out = new HashMap<>();
        enrollments.forEach((id, e) -> {
            Subscription sub = subs.get(e.getSubscriptionId());
            if (sub != null && programs.containsKey(sub.getProgramId())) out.put(id, programs.get(sub.getProgramId()));
        });
        return out;
    }

    // ── Small helpers ───────────────────────────────────────────────────────────────────────────────────

    private static long count(List<TherapySession> list, TherapySessionStatus status) {
        return list.stream().filter(s -> s.getStatus() == status).count();
    }

    private static boolean hasNotes(TherapySession s) {
        return s.getStatus() != TherapySessionStatus.CANCELLED
                && (notBlank(s.getNotes()) || notBlank(s.getFeedback())
                || notBlank(s.getProgressReport()) || notBlank(s.getChecklistNotes()));
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static String pct(long part, long whole) {
        return whole <= 0 ? "--" : Math.round(part * 100.0 / whole) + "%";
    }

    private static String timeRange(LocalTime start, LocalTime end) {
        return start.format(TIME_FMT) + " - " + end.format(TIME_FMT);
    }

    private static String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private static String statusLabel(TherapySessionStatus status, boolean night) {
        return switch (status) {
            case SCHEDULED -> night ? "Not marked" : "Scheduled";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
            case NO_SHOW -> "No show";
            case PENDING_RESCHEDULE -> "Reschedule pending";
            case CANCELLATION_REQUESTED -> "Cancellation requested";
        };
    }

    private static String statusColor(TherapySessionStatus status) {
        return switch (status) {
            case COMPLETED -> "#047857";
            case CANCELLED, NO_SHOW -> "#b91c1c";
            default -> "#b45309";
        };
    }

    private static String esc(String v) {
        return v == null ? "" : HtmlUtils.htmlEscape(v);
    }

    private static String card() {
        return "<div style=\"background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:20px;margin-bottom:16px;\">";
    }

    private static String stat(String value, String label) {
        if (value.isEmpty() && label.isEmpty()) return "<td style=\"border:1px solid #d1d5db;\"></td>";
        return "<td align=\"center\" style=\"border:1px solid #d1d5db;padding:12px 4px;width:25%;\">"
                + "<div style=\"font-size:20px;font-weight:700;color:#1e3a8a;\">" + esc(value) + "</div>"
                + "<div style=\"font-size:12px;color:#6b7280;\">" + esc(label) + "</div></td>";
    }

    private static void statRow(StringBuilder h, String... cells) {
        h.append("<tr>");
        for (String c : cells) h.append(c);
        h.append("</tr>");
    }

    private static String th(String label) {
        return "<th style=\"border:1px solid #d1d5db;\">" + label + "</th>";
    }

    private static String td(long value) {
        return "<td align=\"center\" style=\"border:1px solid #d1d5db;\">" + value + "</td>";
    }

    private static ZoneId zoneOf(Organisation org) {
        try {
            return ZoneId.of(org.getTimezone());
        } catch (DateTimeException | NullPointerException ex) {
            return ZoneId.of("UTC");
        }
    }
}

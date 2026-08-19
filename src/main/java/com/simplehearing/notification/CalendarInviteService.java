package com.simplehearing.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds RFC 5545 iCalendar payloads so meetings land in Google/Outlook/Apple calendars
 * rather than only living inside the app.
 *
 * The UID stays fixed for the life of a meeting and SEQUENCE increases on every change —
 * that pairing is what makes a client update the existing entry instead of adding a second one.
 */
@Service
public class CalendarInviteService {

    private static final DateTimeFormatter UTC_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /** Clinic-local zone. Times are stored as wall-clock, so they need a zone to become instants. */
    private final ZoneId zoneId;

    public CalendarInviteService(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.zoneId = ZoneId.of(timezone);
    }

    public enum Method { REQUEST, CANCEL }

    public record Attendee(String name, String email) {}

    /**
     * @param uid      stable identifier for the event
     * @param sequence revision number — must increase whenever the event changes
     */
    public String build(String uid,
                        int sequence,
                        Method method,
                        String summary,
                        String description,
                        String location,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime,
                        String organiserName,
                        String organiserEmail,
                        List<Attendee> attendees) {

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n")
           .append("VERSION:2.0\r\n")
           .append("PRODID:-//Simple Hearing//Review Meetings//EN\r\n")
           .append("CALSCALE:GREGORIAN\r\n")
           .append("METHOD:").append(method.name()).append("\r\n")
           .append("BEGIN:VEVENT\r\n")
           .append("UID:").append(uid).append("\r\n")
           .append("SEQUENCE:").append(sequence).append("\r\n")
           .append("DTSTAMP:").append(utcStamp(ZonedDateTime.now(zoneId))).append("\r\n")
           .append("DTSTART:").append(utcStamp(date, startTime)).append("\r\n")
           .append("DTEND:").append(utcStamp(date, endTime)).append("\r\n")
           .append(fold("SUMMARY:" + escape(summary))).append("\r\n");

        if (description != null && !description.isBlank()) {
            ics.append(fold("DESCRIPTION:" + escape(description))).append("\r\n");
        }
        if (location != null && !location.isBlank()) {
            ics.append(fold("LOCATION:" + escape(location))).append("\r\n");
        }

        ics.append(fold("ORGANIZER;CN=" + escape(organiserName) + ":mailto:" + organiserEmail)).append("\r\n");

        for (Attendee a : attendees) {
            ics.append(fold("ATTENDEE;CN=" + escape(a.name())
                    + ";ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:" + a.email()))
               .append("\r\n");
        }

        ics.append("STATUS:").append(method == Method.CANCEL ? "CANCELLED" : "CONFIRMED").append("\r\n")
           .append("END:VEVENT\r\n")
           .append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    private String utcStamp(LocalDate date, LocalTime time) {
        return utcStamp(ZonedDateTime.of(LocalDateTime.of(date, time), zoneId));
    }

    private String utcStamp(ZonedDateTime zdt) {
        return zdt.withZoneSameInstant(ZoneId.of("UTC")).format(UTC_STAMP);
    }

    /** Commas, semicolons, backslashes and newlines are structural in iCalendar and must be escaped. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace(";", "\\;")
                    .replace(",", "\\,")
                    .replace("\n", "\\n")
                    .replace("\r", "");
    }

    /**
     * RFC 5545 caps lines at 75 <em>octets</em>; longer ones continue on a line starting
     * with a space. Counting octets rather than characters matters here because names and
     * clinic titles routinely carry non-ASCII — an em dash alone is three bytes.
     * Code points are never split across a fold.
     */
    private static String fold(String line) {
        if (utf8Length(line) <= 75) return line;

        StringBuilder folded = new StringBuilder();
        int octets = 0;
        int limit = 75;                       // continuation lines lose one octet to the leading space

        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int size = utf8Length(new String(Character.toChars(codePoint)));

            if (octets + size > limit) {
                folded.append("\r\n ");
                octets = 0;
                limit = 74;
            }
            folded.append(line, i, i + charCount);
            octets += size;
            i += charCount;
        }
        return folded.toString();
    }

    private static int utf8Length(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
}

package com.simplehearing.notification;

import com.simplehearing.user.enums.Role;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties props;

    public EmailService(Optional<JavaMailSender> mailSender, EmailProperties props) {
        this.mailSender = mailSender.orElse(null);
        this.props = props;
        if (this.mailSender == null) {
            log.warn("No JavaMailSender configured — email sending is disabled");
        }
    }

    @Async
    public void sendInvitationEmail(String to, String acceptPath, Role role, String orgName, String clinicName) {
        String clinicSection = (clinicName != null && !clinicName.isBlank())
                ? "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">You'll be based at <strong style=\"color:#111827;\">" + clinicName + "</strong>.</p>"
                : "";
        String clinicSubtitle = (clinicName != null && !clinicName.isBlank()) ? " &middot; " + clinicName : "";
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("ORG_NAME", orgName);
        vars.put("ROLE", formatRole(role));
        vars.put("ACCEPT_LINK", props.getBaseUrl() + acceptPath);
        vars.put("EXPIRY_HOURS", "72");
        vars.put("LOGO_URL", props.getBaseUrl() + "/logo.png");
        vars.put("CLINIC_SECTION", clinicSection);
        vars.put("CLINIC_SUBTITLE", clinicSubtitle);
        String html = fillStubs(loadTemplate("invitation"), vars);
        send(to, "You're invited to join " + orgName, html);
    }

    @Async
    public void sendNewInquiryNotification(List<String> recipients, String name, String phone,
                                            String email, String reason, String preferredTime,
                                            String orgName) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("ORG_NAME", orgName);
        vars.put("LOGO_URL", props.getBaseUrl() + "/logo.png");
        vars.put("NAME", name);
        vars.put("PHONE", phone);
        vars.put("EMAIL", email != null && !email.isBlank() ? email : "Not provided");
        vars.put("REASON", reason != null && !reason.isBlank() ? reason : "Not provided");
        vars.put("PREFERRED_TIME", preferredTime != null ? preferredTime.charAt(0) + preferredTime.substring(1).toLowerCase() : "No preference");
        vars.put("DASHBOARD_URL", props.getBaseUrl() + "/inquiries");
        String html = fillStubs(loadTemplate("new-inquiry"), vars);
        String subject = "New consultation request from " + name;
        for (String to : recipients) {
            send(to, subject, html);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName, String orgName) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("FIRST_NAME", firstName);
        vars.put("ORG_NAME", orgName);
        vars.put("LOGIN_URL", props.getBaseUrl() + "/login");
        vars.put("LOGO_URL", props.getBaseUrl() + "/logo.png");
        String html = fillStubs(loadTemplate("welcome"), vars);
        send(to, "Welcome to " + orgName, html);
    }

    @Async
    public void sendAppointmentReminderEmail(String to, String patientName, String therapistName,
                                              String date, String time, String clinicName,
                                              String orgName) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("ORG_NAME", orgName);
        vars.put("LOGO_URL", props.getBaseUrl() + "/logo.png");
        vars.put("PATIENT_NAME", patientName);
        vars.put("THERAPIST_NAME", therapistName);
        vars.put("DATE", date);
        vars.put("TIME", time);
        vars.put("CLINIC_NAME", clinicName);
        String html = fillStubs(loadTemplate("appointment-reminder"), vars);
        send(to, "Appointment confirmed — " + date, html);
    }

    @Async
    public void sendLeaveStatusEmail(String to, String therapistName, String leaveDate,
                                      String leaveType, String status, String reviewerName,
                                      String orgName) {
        boolean approved = "APPROVED".equalsIgnoreCase(status);
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("ORG_NAME", orgName);
        vars.put("LOGO_URL", props.getBaseUrl() + "/logo.png");
        vars.put("THERAPIST_NAME", therapistName);
        vars.put("LEAVE_DATE", leaveDate);
        vars.put("LEAVE_TYPE", formatLeaveType(leaveType));
        vars.put("STATUS", status.toUpperCase());
        vars.put("STATUS_LABEL", approved ? "approved" : "rejected");
        vars.put("STATUS_COLOR", approved ? "#166534" : "#991b1b");
        vars.put("STATUS_BG", approved ? "#dcfce7" : "#fee2e2");
        vars.put("STATUS_BORDER", approved ? "#bbf7d0" : "#fecaca");
        vars.put("REVIEWER_NAME", reviewerName);
        String html = fillStubs(loadTemplate("leave-status"), vars);
        send(to, "Leave request " + status.toLowerCase() + " — " + leaveDate, html);
    }

    private String loadTemplate(String name) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + name + ".html");
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Email template not found: " + name, e);
        }
    }

    private String fillStubs(String template, Map<String, String> vars) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private void send(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.warn("Email skipped (no SMTP configured) — to={} subject={}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(props.getFromAddress(), props.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String formatRole(Role role) {
        return switch (role) {
            case THERAPIST -> "Therapist";
            case DOCTOR -> "Doctor";
            case PARENT -> "Parent";
            case PATIENT -> "Patient";
            case ADMIN -> "Admin";
            case OFFICE_ADMIN -> "Office Admin";
            case BUSINESS_OWNER -> "Business Owner";
        };
    }

    private String formatLeaveType(String leaveType) {
        return switch (leaveType) {
            case "FULL_DAY" -> "Full Day";
            case "HALF_DAY" -> "Half Day";
            default -> leaveType;
        };
    }
}

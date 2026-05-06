package com.simplehearing.inquiry.enums;

public enum InquiryActionOutcome {

    // ── Call outcomes (NEW, ATTEMPTED_CONTACT, CONTACTED) ─────────────────────
    NO_ANSWER,          // → ATTEMPTED_CONTACT
    SPOKE_NO_PROGRESS,  // → CONTACTED
    APPOINTMENT_BOOKED, // → CONSULTATION_SCHEDULED  (requires appointmentDate)

    // ── Consultation outcomes (CONSULTATION_SCHEDULED) ─────────────────────────
    REMINDER_SENT,      // → stays CONSULTATION_SCHEDULED
    VISITED,            // → VISITED
    NO_SHOW,            // → CONTACTED
    CANCELLED,          // → DROPPED

    // ── Visit outcomes (VISITED) ───────────────────────────────────────────────
    SCHEDULE_FOLLOWUP,  // → CONSULTATION_SCHEDULED  (requires appointmentDate)

    // ── Terminal / recovery ────────────────────────────────────────────────────
    DROPPED,            // → DROPPED  (valid from NEW, ATTEMPTED_CONTACT, CONTACTED, VISITED)
    REOPEN              // → NEW      (valid from DROPPED, DISCONTINUED)
}

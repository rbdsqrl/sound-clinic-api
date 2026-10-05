package com.simplehearing.session.enums;

/** What kind of thing happened to a session, for the Activity Log. */
public enum SessionActivityType {
    SESSION_CREATED,
    STATUS_CHANGED,
    NOTES_SAVED,
    NOTES_EDITED,
    CHECKLIST_SAVED,
    ATTACHMENT_ADDED,
    RESCHEDULED,
    RESCHEDULE_REQUESTED,
    CANCELLATION_REQUESTED,
    CANCELLATION_APPROVED,
    CANCELLATION_REJECTED
}

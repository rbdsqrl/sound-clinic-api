package com.simplehearing.evidence.enums;

/** Why a therapist couldn't upload a video — categorised so analytics can tell causes apart. */
public enum EvidenceReason {
    DEVICE_PROBLEM,
    POOR_NETWORK,
    STORAGE_FULL,
    FILE_TOO_LARGE,
    APP_ERROR,
    CHILD_UNWELL_OR_UNCOOPERATIVE,
    NO_CONSENT,
    OTHER
}

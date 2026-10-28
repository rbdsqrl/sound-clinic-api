package com.simplehearing.enrollment.enums;

/** Admin-managed lifecycle label of a program. Only RENEWAL_DUE ("Needs Renewal") programs get renewal reminder emails. */
public enum ProgramStatus {
    ACTIVE,
    RENEWAL_DUE,
    COMPLETED_DISCONTINUED
}

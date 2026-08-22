package com.simplehearing.inquiry.enums;

/** How an inquiry reached the clinic. */
public enum InquirySource {
    /** Submitted through the public website form. */
    WEBSITE,
    /** Someone arrived at the clinic and staff took their details. */
    WALK_IN,
    /** A call handled by staff, recorded by hand. */
    PHONE
}

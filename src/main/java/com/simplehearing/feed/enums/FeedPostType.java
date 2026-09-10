package com.simplehearing.feed.enums;

public enum FeedPostType {
    /** An ordinary clinic-wide announcement — the default. */
    POST,
    /** Minutes of a meeting — staff-only visibility, and never shown in the regular Feed list;
     *  it gets its own section on the Dashboard instead. */
    MOM
}

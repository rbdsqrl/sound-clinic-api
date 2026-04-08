package com.simplehearing.exception;

public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String date, String slot) {
        super("Time slot '" + slot + "' on " + date + " is already booked. Please choose another slot.");
    }
}

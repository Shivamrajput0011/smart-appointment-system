package com.yuvaintern.sams.exception;

/** Thrown when the same pet already has an active appointment at the same date and time (HTTP 409). */
public class DuplicateAppointmentException extends RuntimeException {

    public DuplicateAppointmentException(String message) {
        super(message);
    }
}

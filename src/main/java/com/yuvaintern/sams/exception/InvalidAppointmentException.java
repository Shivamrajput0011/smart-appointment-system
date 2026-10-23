package com.yuvaintern.sams.exception;

/** Thrown when an appointment breaks a business rule, e.g. a date in the past (HTTP 400). */
public class InvalidAppointmentException extends RuntimeException {

    public InvalidAppointmentException(String message) {
        super(message);
    }
}

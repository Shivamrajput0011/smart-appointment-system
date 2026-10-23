package com.yuvaintern.sams.exception;

/** Thrown when a pet or appointment with the requested ID does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

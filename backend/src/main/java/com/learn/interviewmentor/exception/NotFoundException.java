package com.learn.interviewmentor.exception;

/** Thrown when an id doesn't exist. Mapped to HTTP 404 by GlobalExceptionHandler. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

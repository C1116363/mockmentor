package com.learn.interviewmentor.exception;

/** Thrown when the caller asks for something illegal. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

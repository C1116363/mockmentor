package com.learn.interviewmentor.exception;

/**
 * You are logged in, but this particular thing is not yours. Mapped to HTTP 403.
 *
 * 401 means "I don't know who you are", 403 means "I know exactly who you are
 * and you still can't". Getting these two the right way round matters.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

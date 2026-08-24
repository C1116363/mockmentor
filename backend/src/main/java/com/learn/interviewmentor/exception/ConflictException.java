package com.learn.interviewmentor.exception;

/**
 * The request was well formed and you were allowed to make it - the world just
 * moved on since you last looked. Mapped to HTTP 409.
 *
 * The distinction from 400 is worth keeping: 400 means "fix your request and try
 * again", 409 means "your request was fine, re-read the current state". Two
 * students racing for the same 3 PM slot is the textbook case - neither of them
 * did anything wrong.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

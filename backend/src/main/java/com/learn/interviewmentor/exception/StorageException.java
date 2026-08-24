package com.learn.interviewmentor.exception;

/**
 * The disk let us down: the upload directory is gone, full, or not writable.
 * Mapped to HTTP 500.
 *
 * This is deliberately NOT a BadRequestException. The caller did nothing wrong,
 * so telling them "bad request" would send them off re-picking a file that was
 * fine. It is our problem, it gets a 500, and it gets logged with a stack trace
 * so somebody actually goes and looks at the server.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

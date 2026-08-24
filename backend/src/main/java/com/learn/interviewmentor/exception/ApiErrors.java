package com.learn.interviewmentor.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.interviewmentor.common.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Writes an {@link ApiResult} failure straight to the servlet response.
 *
 * Requests rejected by the security filter chain never reach Spring MVC, so no
 * @RestControllerAdvice will ever see them - the JSON has to be written by hand.
 * This is that hand, and it writes the same {@link ApiResult} envelope
 * {@link GlobalExceptionHandler} does, so a client cannot tell the two apart.
 *
 * Without this class the shape was spelled out three times, and a field added to
 * one copy silently went missing from the others. The client parses one shape;
 * there should be one place that builds it.
 *
 * @see com.learn.interviewmentor.controller.ApiErrorSchema the Swagger-facing
 *      description of this same shape
 */
public final class ApiErrors {

    /** Records serialise field-by-field in declaration order, so no config needed. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiErrors() {
    }

    /**
     * Writes the body straight to the response, for the filter-chain handlers.
     *
     * Sets the status before touching the stream: once the first byte is written
     * the response is committed and the status can no longer be changed.
     */
    public static void write(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpStatus status,
                             String message) throws IOException {

        if (response.isCommitted()) {
            // Something already started writing. Anything we add now would be
            // appended to a half-sent response, producing invalid JSON.
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        MAPPER.writeValue(response.getOutputStream(),
                ApiResult.failure(status.value(), message, request.getRequestURI()));
    }
}

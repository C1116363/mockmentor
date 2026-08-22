package com.learn.interviewmentor.controller;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Documentation-only shape of the JSON that GlobalExceptionHandler returns.
 *
 * The handler builds a plain Map, which Swagger cannot introspect, so this
 * record exists purely so the error responses show real fields in the UI
 * instead of an empty object. Nothing constructs it at runtime.
 */
@Schema(name = "ApiError", description = "Standard error response")
public record ApiErrorSchema(

        @Schema(description = "When the error happened", example = "2026-08-22T13:15:30.123")
        String timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "What went wrong, in plain English",
                example = "An account with rahul@example.com already exists")
        String message,

        @Schema(description = "Only present on validation failures: field name -> message",
                example = "{\"password\": \"Password must be at least 8 characters\"}")
        Map<String, String> fieldErrors
) {
}

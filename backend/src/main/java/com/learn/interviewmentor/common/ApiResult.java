package com.learn.interviewmentor.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The one envelope every endpoint returns, success or failure.
 *
 * <pre>
 * success:  { "success": true,  "status": 200, "message": "...", "data": { ... } }
 * failure:  { "success": false, "status": 404, "message": "No plan with id 9", "data": null }
 * </pre>
 *
 * <h2>Why an envelope at all</h2>
 * A client can tell success from failure by reading one boolean, in one place,
 * for every call it ever makes - instead of every caller knowing which endpoint
 * returns a bare object, which returns a bare list, and which returns nothing.
 * {@code fieldErrors} rides in the same shape, so form validation is handled by
 * the same branch as everything else.
 *
 * <h2>The cost, stated honestly</h2>
 * The payload is one level deeper, so clients unwrap {@code .data}. That is a
 * real tax, and it is paid once: in the frontend it lives in a single line of
 * {@code api/client.js}, and nothing above that line knows the envelope exists.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@code data} is null on failure. Never a partial result - a client that
 *       reads {@code data} after checking {@code success} must not find half an
 *       answer there.</li>
 *   <li>{@code message} is written for a person on failure, and is usually null
 *       on success. A success message is for something worth saying out loud
 *       ("Price updated - students see it on their next load"), not narration of
 *       what the caller just asked for.</li>
 *   <li>{@code fieldErrors} appears only on a validation failure, and is
 *       omitted - not empty - otherwise.</li>
 *   <li>{@code status} repeats the HTTP status inside the body. Redundant over
 *       HTTP, and worth it: logs, proxies and message queues carry the body long
 *       after the status line is gone.</li>
 * </ul>
 *
 * <h2>Why not called ApiResponse</h2>
 * Because {@code io.swagger.v3.oas.annotations.responses.ApiResponse} already is,
 * and every controller imports it to document its status codes. Java has no
 * import aliases, so sharing the simple name would force one of the two to be
 * written fully-qualified in every single signature. The name is worth less than
 * the readability.
 *
 * @param <T> the payload type on success
 */
/*
 * Null fields are dropped from the JSON. Without this every success carries
 * "message": null, "fieldErrors": null, "path": null - three keys that say
 * nothing, on every response the API ever sends. `success` and `status` are
 * primitives so they are always present, which is what a client branches on.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiResult", description = "Standard envelope returned by every endpoint.")
public record ApiResult<T>(

        @Schema(description = "true when the call succeeded. Check this first, always.",
                example = "true")
        boolean success,

        @Schema(description = "HTTP status, repeated in the body so it survives logging",
                example = "200")
        int status,

        @Schema(description = "Plain-English message. On a failure this is safe to show a user; "
                + "on a 5xx it ends with a reference code to quote.")
        String message,

        @Schema(description = "The payload. Always null on failure.")
        T data,

        @Schema(description = "Only on a validation failure: field name -> what is wrong with it",
                example = "{\"password\": \"Password must be at least 8 characters\"}")
        Map<String, String> fieldErrors,

        @Schema(description = "The URI that was called", example = "/api/plans/1")
        String path,

        String timestamp
) {

    // ---------------- success ----------------

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, 200, null, data, null, null, now());
    }

    /** Use when there is something worth telling the user, beyond "it worked". */
    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(true, 200, message, data, null, null, now());
    }

    public static <T> ApiResult<T> created(T data) {
        return new ApiResult<>(true, 201, null, data, null, null, now());
    }

    public static <T> ApiResult<T> created(T data, String message) {
        return new ApiResult<>(true, 201, message, data, null, null, now());
    }

    // ---------------- failure ----------------

    /**
     * Built by {@code GlobalExceptionHandler}, which is the only thing that
     * should be constructing failures - a controller returning a hand-made
     * failure envelope means an exception went uncaught somewhere it shouldn't.
     */
    public static <T> ApiResult<T> failure(int status, String message, String path) {
        return new ApiResult<>(false, status, message, null, null, path, now());
    }

    public static <T> ApiResult<T> validationFailure(String message,
                                                       Map<String, String> fieldErrors,
                                                       String path) {
        return new ApiResult<>(false, 400, message, null, fieldErrors, path, now());
    }

    private static String now() {
        return LocalDateTime.now().toString();
    }
}

package com.learn.interviewmentor.exception;

import com.learn.interviewmentor.common.ApiResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One place that turns exceptions into clean JSON error responses.
 *
 * The rule this file exists to enforce: <b>every</b> failure leaves through here
 * with a {@code message} the frontend can show a human. Anything that escapes
 * uncaught gets Spring's default error body, which in Boot 3 omits the message
 * entirely - the client then has nothing to display but "something went wrong".
 * So the last handler catches {@code Exception} itself.
 *
 * <h2>Two rules for the messages</h2>
 * <ol>
 *   <li><b>4xx messages are for the user.</b> They describe what to do next, and
 *       they are safe to show on screen.</li>
 *   <li><b>5xx messages tell the user nothing.</b> Constraint names, SQL, class
 *       names and stack traces are reconnaissance for an attacker and noise for
 *       everyone else. The detail goes to the log; the caller gets a reference
 *       code to quote.</li>
 * </ol>
 *
 * Note what is <i>not</i> here: requests rejected by the security filter chain
 * never reach Spring MVC, so 401s (and filter-level 403s) are produced by
 * {@code RestAuthenticationEntryPoint} and {@code RestAccessDeniedHandler}
 * instead. All three build their body through {@link ApiErrors}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Read from config so the message can't drift from the limit Tomcat enforces. */
    private final String maxFileSize;

    public GlobalExceptionHandler(
            @Value("${spring.servlet.multipart.max-file-size:5MB}") String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    // ------------------------------------------------------------------
    // Our own exceptions
    // ------------------------------------------------------------------

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(NotFoundException ex,
                                                             HttpServletRequest request) {
        return clientError(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResult<Void>> handleBadRequest(BadRequestException ex,
                                                                HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex);
    }

    /** Logged in, but not allowed -> 403 Forbidden. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResult<Void>> handleForbidden(ForbiddenException ex,
                                                              HttpServletRequest request) {
        // Worth a warn rather than a debug: a burst of these is somebody probing
        // other people's ids.
        log.warn("403 on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.failure(HttpStatus.FORBIDDEN.value(), ex.getMessage(), request.getRequestURI()));
    }

    /** The world moved on between read and write -> 409 Conflict. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResult<Void>> handleConflict(ConflictException ex,
                                                             HttpServletRequest request) {
        return clientError(HttpStatus.CONFLICT, ex.getMessage(), request, ex);
    }

    /** The disk failed on us. Our fault, so 500 - see StorageException. */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResult<Void>> handleStorage(StorageException ex,
                                                            HttpServletRequest request) {
        return serverError("Could not store the file. Please try again.", request, ex);
    }

    /**
     * The payment gateway is unreachable or misconfigured -> 502 Bad Gateway.
     *
     * The message the student sees is fixed, and says the one thing they
     * actually need to know: their money has not been taken. A payment error
     * with a vague message is the kind that makes somebody pay again to be
     * safe - which is the outcome worth designing against. The detail goes to
     * the log with a reference code, as it does for any 5xx.
     */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiResult<Void>> handlePaymentGateway(PaymentGatewayException ex,
                                                                HttpServletRequest request) {
        String reference = reference();
        log.error("Payment gateway failure [{}] on {} {}",
                reference, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResult.failure(HttpStatus.BAD_GATEWAY.value(),
                        "We couldn't reach the payment gateway, so nothing has been charged. "
                                + "Try again in a moment, or pay by UPI instead. "
                                + "(reference " + reference + ")",
                        request.getRequestURI()));
    }

    // ------------------------------------------------------------------
    // Authentication and authorisation (the MVC-level half)
    // ------------------------------------------------------------------

    /** Wrong email or password -> 401 Unauthorized. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthentication(AuthenticationException ex,
                                                                   HttpServletRequest request) {
        log.info("Failed authentication on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.failure(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Thrown by @PreAuthorize when the role doesn't match.
     *
     * The message is fixed on purpose. Spring's own text names the expected
     * authority, which tells a prober exactly which role to go after.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException ex,
                                                                 HttpServletRequest request) {
        log.warn("Access denied on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.failure(HttpStatus.FORBIDDEN.value(), "You do not have permission to do that", request.getRequestURI()));
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /** Fires when a @Valid DTO fails its constraints. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        // A DTO-level @AssertTrue has no field to attach to; don't lose it.
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        return validationFailed(fieldErrors, request);
    }

    /**
     * The same thing for constraints on parameters rather than a DTO - a
     * {@code @Min} on a @RequestParam, say. Different exception, same response,
     * because the client should not have to care which one it tripped.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                        HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (ex.getConstraintViolations() != null) {
            ex.getConstraintViolations().forEach(violation -> {
                // Property path is "method.argName"; only the last node is a
                // name the caller would recognise.
                String path = String.valueOf(violation.getPropertyPath());
                String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                fieldErrors.putIfAbsent(field, violation.getMessage());
            });
        }
        return validationFailed(fieldErrors, request);
    }

    /** Spring 6.1+ raises this for @Valid on individual handler parameters. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResult<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                fieldErrors.putIfAbsent(
                        result.getMethodParameter().getParameterName(),
                        error.getDefaultMessage())));

        return validationFailed(fieldErrors, request);
    }

    // ------------------------------------------------------------------
    // Malformed requests - the caller got the HTTP itself wrong
    // ------------------------------------------------------------------

    /**
     * {@code /api/payments/abc/screenshot} where a Long was expected, or a date
     * that isn't a date.
     *
     * We name the parameter and the value but never the Java type - "must be of
     * type java.lang.Long" means nothing to a frontend developer and quietly
     * advertises the stack.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                 HttpServletRequest request) {
        String message = "'%s' is not a valid value for %s".formatted(ex.getValue(), ex.getName());
        return clientError(HttpStatus.BAD_REQUEST, message, request, ex);
    }

    /** Body missing, truncated, or not JSON at all. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                   HttpServletRequest request) {
        // Jackson's own message quotes the offending JSON and the target class.
        // Useful in the log, unwise in a response.
        log.debug("Unreadable body on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.badRequest().body(ApiResult.failure(HttpStatus.BAD_REQUEST.value(), "The request body is missing or is not valid JSON", request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException ex,
                                                                 HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST,
                "Required parameter '%s' is missing".formatted(ex.getParameterName()), request, ex);
    }

    /** A multipart form arrived without one of its expected parts. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingPart(MissingServletRequestPartException ex,
                                                                HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST,
                "Required part '%s' is missing".formatted(ex.getRequestPartName()), request, ex);
    }

    /**
     * 405 must carry an {@code Allow} header - the RFC requires it, and it is
     * what tells a client which methods the URL does support.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String message = "%s is not supported on this endpoint".formatted(ex.getMethod());
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        log.debug("405 on {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers)
                .body(ApiResult.failure(HttpStatus.METHOD_NOT_ALLOWED.value(), message, request.getRequestURI()));
    }

    /**
     * Almost always the multipart mistake: sending JSON to the screenshot
     * upload, or setting Content-Type by hand on a FormData request so the
     * boundary goes missing.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        String message = ex.getContentType() == null
                ? "This endpoint needs a Content-Type header"
                : "This endpoint does not accept %s".formatted(ex.getContentType());
        return clientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, request, ex);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResult<Void>> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                  HttpServletRequest request) {
        return clientError(HttpStatus.NOT_ACCEPTABLE,
                "This endpoint cannot produce any of the types you asked for", request, ex);
    }

    /** Upload over spring.servlet.multipart.max-file-size. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                                                   HttpServletRequest request) {
        // Tomcat rejects the request before the controller runs, so
        // ScreenshotStorage's own size check never gets a look in - this is the
        // only place the user can be told what the limit is.
        return clientError(HttpStatus.PAYLOAD_TOO_LARGE,
                "That file is too large. The limit is %s.".formatted(maxFileSize), request, ex);
    }

    /** Anything else wrong with the multipart body itself. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResult<Void>> handleMultipart(MultipartException ex,
                                                              HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST,
                "The upload was not a valid multipart/form-data request", request, ex);
    }

    /** No endpoint at that URL. Boot 3 raises one of these two. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResult<Void>> handleNoHandler(Exception ex,
                                                              HttpServletRequest request) {
        return clientError(HttpStatus.NOT_FOUND,
                "No endpoint for %s %s".formatted(request.getMethod(), request.getRequestURI()),
                request, ex);
    }

    // ------------------------------------------------------------------
    // Database
    // ------------------------------------------------------------------

    /**
     * A unique or foreign-key constraint said no - two signups racing for the
     * same email, most likely.
     *
     * The exception message contains the constraint name and often the offending
     * value, so it is logged and not returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                  HttpServletRequest request) {
        log.warn("Constraint violation on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.failure(HttpStatus.CONFLICT.value(), "That conflicts with something that already exists. Reload and try again.",
                request.getRequestURI()));
    }

    /** Two writers touched the same row. Retrying usually fixes it. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResult<Void>> handleOptimisticLocking(OptimisticLockingFailureException ex,
                                                                      HttpServletRequest request) {
        return clientError(HttpStatus.CONFLICT,
                "Someone else changed that at the same time. Reload and try again.", request, ex);
    }

    /**
     * Everything else from the data layer: MySQL is down, the pool is exhausted,
     * a query timed out.
     *
     * 503 rather than 500 - it says "try again later", which is true, and it is
     * the status a load balancer or client retry policy is looking for. Declared
     * after the two above; Spring always picks the most specific handler.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResult<Void>> handleDataAccess(DataAccessException ex,
                                                                HttpServletRequest request) {
        String reference = reference();
        log.error("Data access failure [{}] on {} {}",
                reference, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResult.failure(HttpStatus.SERVICE_UNAVAILABLE.value(), "The service is temporarily unavailable. Please try again in a moment. "
                                + "(reference " + reference + ")",
                        request.getRequestURI()));
    }

    // ------------------------------------------------------------------
    // Catch-alls
    // ------------------------------------------------------------------

    /** Something threw one of Spring's own status-carrying exceptions. Honour it. */
    @ExceptionHandler({ResponseStatusException.class, ErrorResponseException.class})
    public ResponseEntity<ApiResult<Void>> handleResponseStatus(ErrorResponseException ex,
                                                                   HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getBody().getDetail() != null ? ex.getBody().getDetail() : status.getReasonPhrase();

        if (status.is5xxServerError()) {
            return serverError(message, request, ex);
        }
        return clientError(status, message, request, ex);
    }

    /**
     * An argument was rejected deep in the call stack. Conventionally a 400, and
     * logged with its stack because it is just as often our bug as the caller's.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException ex,
                                                                    HttpServletRequest request) {
        log.warn("Illegal argument on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.badRequest().body(ApiResult.failure(HttpStatus.BAD_REQUEST.value(), "That request could not be processed", request.getRequestURI()));
    }

    /**
     * The backstop. Nothing gets past this, which is the whole point.
     *
     * Without it an unforeseen NullPointerException returns Spring's default
     * error body - no {@code message} field at all in Boot 3 - and the frontend,
     * finding nothing to show, tells the user the backend is down. A 500 that
     * lies about the cause is worse than a 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex,
                                                               HttpServletRequest request) {
        return serverError("Something went wrong on our end. Please try again.", request, ex);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** 4xx: the message is the caller's to read, and no stack trace is logged. */
    private ResponseEntity<ApiResult<Void>> clientError(HttpStatus status,
                                                        String message,
                                                        HttpServletRequest request,
                                                        Exception ex) {
        log.debug("{} on {} {}: {}",
                status.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResult.failure(status.value(), message, request.getRequestURI()));
    }

    /**
     * 5xx: the caller gets a reference code and nothing else; the log gets
     * everything. Grep the log for the code the user quotes and the stack trace
     * for their exact request is right there.
     */
    private ResponseEntity<ApiResult<Void>> serverError(String message,
                                                            HttpServletRequest request,
                                                            Exception ex) {
        String reference = reference();
        log.error("Unhandled failure [{}] on {} {}",
                reference, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        message + " (reference " + reference + ")", request.getRequestURI()));
    }

    private ResponseEntity<ApiResult<Void>> validationFailed(Map<String, String> fieldErrors,
                                                             HttpServletRequest request) {
        log.debug("Validation failed on {} {}: {}",
                request.getMethod(), request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(ApiResult.validationFailure(
                "Validation failed", fieldErrors, request.getRequestURI()));
    }

    /** Short enough for a user to read down the phone, long enough not to collide. */
    private String reference() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

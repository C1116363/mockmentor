package com.learn.interviewmentor.security;

import com.learn.interviewmentor.exception.ApiErrors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs when a request arrives with NO valid authentication.
 *
 * Without this, Spring Security's default for a stateless API is to return 403.
 * That is wrong and it actually breaks the frontend: the browser client clears a
 * stale token when it sees 401, so a 403 would leave the user stuck with an
 * expired token and no way back to the login screen.
 *
 * 401 = "I don't know who you are."
 * 403 = "I know who you are, and you still can't." (that's the other handler)
 *
 * These run inside the filter chain, BEFORE Spring MVC, so @RestControllerAdvice
 * never sees them - we have to write the JSON ourselves. {@link ApiErrors} keeps
 * that JSON identical to what the advice produces.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Debug, not warn: an anonymous hit on a protected URL is the normal
        // first request of every session, not an incident.
        log.debug("401 on {} {}: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        ApiErrors.write(request, response, HttpStatus.UNAUTHORIZED, "You need to log in to do that");
    }
}

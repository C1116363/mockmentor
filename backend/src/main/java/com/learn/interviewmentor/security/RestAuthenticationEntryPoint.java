package com.learn.interviewmentor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * never sees them - we have to write the JSON ourselves.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "You need to log in to do that");

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

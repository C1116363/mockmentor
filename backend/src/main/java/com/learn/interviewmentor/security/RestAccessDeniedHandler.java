package com.learn.interviewmentor.security;

import com.learn.interviewmentor.exception.ApiErrors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs when the caller IS authenticated but their role doesn't allow it -
 * e.g. a student hitting /api/admin/stats.
 *
 * Like the entry point, this is filter-chain territory: it fires before Spring
 * MVC, so the advice never sees it. Body shape comes from {@link ApiErrors} so
 * the two paths cannot drift apart.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // A known user reaching for something above their role is worth seeing.
        log.warn("403 on {} {} for '{}': {}",
                request.getMethod(), request.getRequestURI(),
                request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName(),
                accessDeniedException.getMessage());

        ApiErrors.write(request, response, HttpStatus.FORBIDDEN, "Your role does not allow that");
    }
}

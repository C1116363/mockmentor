package com.learn.interviewmentor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request, before the controller.
 *
 * It looks for "Authorization: Bearer <token>", validates it, and if it is good
 * puts an Authentication into the SecurityContext. Everything downstream
 * (hasRole, @PreAuthorize, @AuthenticationPrincipal) reads from there.
 *
 * If there is no token, it just calls chain.doFilter and moves on - it is not
 * this filter's job to reject anyone. The authorization rules in SecurityConfig
 * do that.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        String email = jwtService.extractEmail(token);

        // Only authenticate if the token was valid AND nobody has already
        // been authenticated earlier in the chain.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException ex) {
                // The token is signed and unexpired, but the account behind it is
                // gone - deleted, or the database was rebuilt while a browser
                // still held a token. Left uncaught this escapes the filter, and
                // an exception thrown in a filter never reaches either the
                // entry point or @RestControllerAdvice: the container turns it
                // straight into a bare 500. Swallowing it leaves the request
                // anonymous, so the entry point answers 401 and the frontend
                // drops the dead token and shows the login screen.
                log.debug("Token for unknown account '{}' on {}", email, request.getRequestURI());
            } catch (DataAccessException ex) {
                // Database unreachable. Same reasoning: don't let it out of the
                // filter. Anonymous here means 401 rather than an HTML 500 page,
                // and the failure is logged loudly for whoever is on call.
                log.error("Could not load '{}' while authenticating {}",
                        email, request.getRequestURI(), ex);
            }
        }

        chain.doFilter(request, response);
    }
}

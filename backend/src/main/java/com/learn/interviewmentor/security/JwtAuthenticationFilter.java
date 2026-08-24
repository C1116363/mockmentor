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
import java.time.LocalDateTime;

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

                if (userDetails.isEnabled() && issuedAfterLastPasswordChange(token, userDetails)) {
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

    /**
     * Refuse a token that predates the account's last password reset.
     *
     * This is what makes "reset your password" mean something. The JWT is
     * stateless and lives 24 hours, so a token an attacker already holds keeps
     * working for a day after the victim resets - the victim does everything
     * right, sees a success message, and is still compromised. There is no
     * server-side session to delete, so the check has to be this: was this
     * token minted before the password changed?
     *
     * <h2>Ties are refused, and that is the safe direction</h2>
     * A JWT's "iat" is whole seconds while the column has millisecond
     * precision, so "issued at 20:55:58" really means "somewhere in
     * [58.000, 58.999]". The only safe reading is the earliest instant in that
     * range - which is what comparing the untruncated values does.
     *
     * The cost is that a token minted in the same second as the reset is
     * refused even though it came after. That is a sub-second window, it can
     * only be hit by logging in the instant the reset completes, and it costs
     * one retry. The alternative - truncating both sides to the second so ties
     * pass - buys that away by accepting every stale token issued in the same
     * second as the reset. An earlier version of this method did exactly that,
     * and a test whose steps all ran inside one second caught it: the old token
     * sailed through a completed password reset.
     *
     * Given a choice between "log in again" and "the attacker stays in", this
     * one is not close.
     */
    private boolean issuedAfterLastPasswordChange(String token, UserDetails userDetails) {
        if (!(userDetails instanceof AppUserDetails details)) {
            return true;
        }
        LocalDateTime changedAt = details.getUser().getPasswordChangedAt();
        if (changedAt == null) {
            // Never reset, so nothing to be older than.
            return true;
        }

        LocalDateTime issuedAt = jwtService.extractIssuedAt(token);
        if (issuedAt == null) {
            // Signed and unexpired but carrying no iat - not something this app
            // issues. Refused rather than trusted: the whole point of the check
            // is that a token cannot opt out of it.
            log.debug("Token for '{}' has no issued-at claim - refusing", details.getUsername());
            return false;
        }

        boolean stale = issuedAt.isBefore(changedAt);

        if (stale) {
            log.info("Refused a token for '{}' issued before their password was reset",
                    details.getUsername());
        }
        return !stale;
    }
}

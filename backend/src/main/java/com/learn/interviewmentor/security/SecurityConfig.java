package com.learn.interviewmentor.security;

import com.learn.interviewmentor.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * The heart of the security setup.
 *
 * Modern Spring Security (6.x) is configured by declaring a SecurityFilterChain
 * bean. If you find a tutorial extending WebSecurityConfigurerAdapter, it is
 * years out of date - that class was removed.
 *
 * @EnableMethodSecurity turns on @PreAuthorize on individual methods.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * BCrypt hashes passwords with a random salt built in, so the same password
     * hashes differently every time. Never store plain passwords, and never use
     * MD5/SHA for them.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Ties "load the user from the DB" together with "check the BCrypt hash". */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** AuthService needs this to verify email + password at login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF protects cookie-based sessions. We are stateless with a
                // Bearer token, so there is nothing for an attacker to ride on.
                .csrf(csrf -> csrf.disable())

                // No HttpSession at all - the token IS the session.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Browsers fire an OPTIONS preflight before real calls.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Signup and login must be reachable without a token,
                        // otherwise nobody could ever get one.
                        .requestMatchers("/api/auth/signup/**", "/api/auth/login").permitAll()

                        // The website has no login, so its stats endpoint
                        // must be open. It returns counts only, never people.
                        .requestMatchers("/api/public/**").permitAll()

                        // Swagger UI and the OpenAPI spec. Fine to leave open on
                        // a learning project; on a real public API you would
                        // lock these down or disable them outside dev.
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**").permitAll()

                        // Anyone logged in can browse the mentor list.
                        .requestMatchers(HttpMethod.GET, "/api/mentors/**").authenticated()

                        // Reading the price list needs only a token - a mentor
                        // or admin looking at the plans page is harmless, and
                        // the marketing site reads /api/public/plans anyway.
                        // BUYING one is students only.
                        .requestMatchers(HttpMethod.POST, "/api/plans/*/enroll").hasRole(Role.STUDENT.name())
                        .requestMatchers("/api/plans/enrollments/*/proof").hasRole(Role.STUDENT.name())
                        .requestMatchers("/api/plans/enrollments/*/cancel").hasRole(Role.STUDENT.name())
                        .requestMatchers(HttpMethod.GET, "/api/plans/**").authenticated()

                        // Study material is addressed to students, and the
                        // service filters each list by who is asking. Admins
                        // reach their own view under /api/admin/materials.
                        .requestMatchers(HttpMethod.GET, "/api/materials/**").authenticated()

                        // Anyone logged in may browse the project catalogue - the
                        // repository path is withheld from the response unless
                        // they hold access, so browsing leaks nothing. Only
                        // students can request or pay for access.
                        .requestMatchers(HttpMethod.POST, "/api/projects/*/request-access")
                                .hasRole(Role.STUDENT.name())
                        .requestMatchers("/api/projects/access/*/proof").hasRole(Role.STUDENT.name())
                        .requestMatchers("/api/projects/access/*/cancel").hasRole(Role.STUDENT.name())
                        .requestMatchers("/api/projects/access/*/github-username")
                                .hasRole(Role.STUDENT.name())
                        .requestMatchers(HttpMethod.GET, "/api/projects/**").authenticated()

                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())

                        // Only students raise requests.
                        .requestMatchers(HttpMethod.POST, "/api/requests").hasRole(Role.STUDENT.name())

                        // The open queue and accepting are mentor-only.
                        .requestMatchers("/api/requests/pending").hasRole(Role.MENTOR.name())
                        .requestMatchers(HttpMethod.PATCH, "/api/requests/*/accept").hasRole(Role.MENTOR.name())
                        .requestMatchers(HttpMethod.PATCH, "/api/requests/*/complete").hasRole(Role.MENTOR.name())

                        // Everything else just needs a valid token. Note the
                        // order matters: the FIRST matching rule wins, so this
                        // catch-all has to stay last.
                        .anyRequest().authenticated()
                )

                // Without this block a missing/expired token would come back as
                // 403 instead of 401, and the frontend would never know to
                // throw the stale token away.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authenticationProvider(authenticationProvider())

                // Our JWT filter has to run before the username/password filter
                // so the SecurityContext is populated by the time authorization
                // rules are evaluated.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS now lives here instead of in a WebMvcConfigurer, because Spring
     * Security's filter chain runs before Spring MVC and would otherwise reject
     * the request before MVC's CORS handling ever ran.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

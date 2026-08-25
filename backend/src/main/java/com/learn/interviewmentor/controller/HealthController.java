package com.learn.interviewmentor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Is this instance actually able to serve traffic?
 *
 * <h2>Why hosting needs this at all</h2>
 * A platform decides whether a container is alive by asking it. Point that
 * check at {@code /} and this API answers <b>401</b> - correct behaviour, since
 * the root needs a token, and fatal as a health check: the platform reads a
 * non-2xx as "dead", kills a perfectly good container, and retries forever
 * while the logs show a clean startup every time.
 *
 * <h2>It checks the database on purpose</h2>
 * A cheaper endpoint that returns 200 as soon as Tomcat is up would report
 * healthy while every real request fails. This app cannot do anything useful
 * without MySQL, so "healthy" has to mean the connection pool can hand out a
 * working connection. {@code SELECT 1} is the cheapest way to ask.
 *
 * <h2>It deliberately says almost nothing</h2>
 * No version, no hostname, no database name, no stack trace. It is
 * unauthenticated and reachable by anyone who finds the URL, and the only
 * audience that needs detail is the log.
 */
@RestController
@RequestMapping("/api/public/health")
@Tag(name = "0. Health", description = "Liveness for hosting platforms and uptime checks.")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(
            summary = "Liveness check",
            description = """
                    `200 {"status":"ok"}` when the app is up **and** the database answers.
                    `503 {"status":"degraded"}` when it cannot reach the database.

                    Point your platform's health check here. Pointing it at `/` gets a 401,
                    which every platform reads as a dead container.

                    Says nothing beyond up or down - it is unauthenticated, so the detail
                    goes to the log instead.
                    """)
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbc.queryForObject("select 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException e) {
            // The reason belongs here, where an operator can read it - not in a
            // response body served to anyone who curls the URL.
            log.error("Health check failed: the database did not answer", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "degraded"));
        }
    }
}

package com.learn.interviewmentor.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What login and signup both return.")
public record AuthResponse(

        @Schema(description = "The JWT. Paste this into the Authorize button at the top of this page.",
                example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJyYWh1bEBleGFtcGxlLmNvbSJ9.abc123")
        String token,

        @Schema(description = "How long the token stays valid, in milliseconds (24 hours)",
                example = "86400000")
        long expiresInMs,

        @Schema(description = "The account you just logged in as")
        UserDto user
) {
}

package com.learn.interviewmentor.dto.auth;

import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * A user as the browser sees them. Notice there is no password field -
 * the hash must never leave the server.
 */
@Schema(description = "A user account. The password hash is deliberately absent.")
public record UserDto(

        @Schema(description = "Database id", example = "5")
        Long id,

        @Schema(description = "Full name", example = "Rahul Sharma")
        String fullName,

        @Schema(description = "Login email", example = "rahul@example.com")
        String email,

        @Schema(description = "Determines which endpoints they can call", example = "STUDENT")
        Role role,

        @Schema(description = "false means an admin has blocked them from logging in", example = "true")
        boolean active,

        @Schema(description = "When the account was created", example = "2026-08-22T12:57:30.810")
        LocalDateTime createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}

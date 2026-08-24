package com.learn.interviewmentor.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "I've forgotten my password - send me a link." */
@Schema(description = "Ask for a password-reset link.")
public record ForgotPasswordDto(

        @NotBlank(message = "Enter your email address")
        @Email(message = "That doesn't look like an email address")
        @Size(max = 190)
        @Schema(example = "rahul@example.com")
        String email
) {
}

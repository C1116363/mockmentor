package com.learn.interviewmentor.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * "Here's the token from my email, and my new password."
 *
 * There is no email field and no user id, deliberately. The token identifies
 * the account on its own - accepting an address alongside it would mean the
 * server had to decide what to do when the two disagree, and the only safe
 * answer is to ignore the address entirely. Better not to accept it.
 */
@Schema(description = "Set a new password using a reset link.")
public record ResetPasswordDto(

        @NotBlank(message = "The reset link is incomplete")
        @Size(max = 200)
        @Schema(description = "The token from the reset link",
                example = "Yk3n8Qm2Lp9Rv4Tx7Wz1Bc5Df6Gh0Jk2Mn4Pq8Rs6U")
        String token,

        @NotBlank(message = "Choose a new password")
        @Size(min = 8, max = 100, message = "Your password must be at least 8 characters")
        @Schema(example = "a-new-password")
        String newPassword
) {
}

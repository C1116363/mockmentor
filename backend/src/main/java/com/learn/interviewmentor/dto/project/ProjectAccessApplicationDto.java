package com.learn.interviewmentor.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A student asking for contributor access.
 *
 * The GitHub username is collected here rather than after payment, so an admin
 * with a verified payment is never sitting waiting for somebody to answer an
 * email before they can grant anything.
 */
@Schema(description = "Request contributor access to one live project.")
public record ProjectAccessApplicationDto(

        /*
         * GitHub's rules, enforced here rather than discovered later: 1-39 chars,
         * alphanumerics and single hyphens, no leading or trailing hyphen. An
         * invalid handle means the grant 404s after the student has already paid.
         */
        @NotBlank(message = "Your GitHub username is needed to give you access")
        @Size(max = 39, message = "GitHub usernames are 39 characters or fewer")
        @Pattern(regexp = "^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$",
                message = "That is not a valid GitHub username - letters, numbers and single hyphens only")
        @Schema(example = "rahul-sharma")
        String githubUsername,

        @Size(max = 2000, message = "Keep it under 2000 characters")
        @Schema(description = "Why you want to work on this, and what you can bring. "
                + "The reviewer reads this.",
                example = "Final year student, comfortable with Spring Boot. I'd like to start "
                        + "with the webhook retry task.")
        String motivation
) {
}

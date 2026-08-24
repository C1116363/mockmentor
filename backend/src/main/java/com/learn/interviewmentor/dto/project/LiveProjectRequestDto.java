package com.learn.interviewmentor.dto.project;

import com.learn.interviewmentor.model.ProjectDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Admin creating or editing a live project. ADMIN only. */
@Schema(description = "Create or replace a live project students can contribute to.")
public record LiveProjectRequestDto(

        @NotBlank(message = "Give the project a name")
        @Size(max = 140, message = "Name must be 140 characters or fewer")
        @Schema(example = "Leegality eSign gateway")
        String name,

        @Size(max = 240, message = "Summary must be 240 characters or fewer")
        @Schema(example = "Production eSign flows used by 200+ banks")
        String summary,

        @Size(max = 5000)
        String description,

        @Size(max = 300)
        @Schema(description = "Comma separated", example = "Java, Spring Boot, MySQL, React")
        String techStack,

        @Size(max = 5000)
        @Schema(description = "One task per line - what a contributor would actually pick up",
                example = "Add retry handling to the webhook dispatcher\nWrite tests for the PDF signer")
        String sampleTasks,

        /*
         * GitHub's own rules: 1-39 characters, alphanumerics and single hyphens,
         * cannot start or end with a hyphen. Validated here because a malformed
         * owner or name means the grant call 404s later, at the worst moment -
         * after somebody has paid.
         */
        @NotBlank(message = "Repository owner is required")
        @Pattern(regexp = "^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$",
                message = "That is not a valid GitHub owner name")
        @Schema(example = "leegality")
        String repoOwner,

        @NotBlank(message = "Repository name is required")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,100}$",
                message = "That is not a valid GitHub repository name")
        @Schema(example = "esign-gateway")
        String repoName,

        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Must start with http:// or https://")
        @Schema(description = "Optional CONTRIBUTING.md or onboarding doc",
                example = "https://github.com/leegality/esign-gateway/blob/main/CONTRIBUTING.md")
        String onboardingUrl,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @DecimalMax(value = "999999.99", message = "That price looks like a typo")
        @Schema(example = "7999.00")
        BigDecimal price,

        @Min(value = 1, message = "Access must last at least a day")
        @Max(value = 3650, message = "Access cannot last more than 10 years")
        @Schema(example = "90")
        int accessDurationDays,

        @Min(value = 1, message = "A project needs at least one seat")
        @Max(value = 500, message = "That many reviewers is not realistic")
        @Schema(description = "How many contributors may hold access at once. "
                + "Leave null for no limit.", example = "5")
        Integer maxContributors,

        @NotNull(message = "Pick a difficulty")
        @Schema(example = "INTERMEDIATE",
                allowableValues = {"BEGINNER", "INTERMEDIATE", "ADVANCED"})
        ProjectDifficulty difficulty,

        @Schema(description = "User id of the senior engineer who reviews PRs on this repo")
        Long leadReviewerId,

        @Schema(description = "Lower numbers appear first")
        int displayOrder,

        @Schema(description = "Inactive projects disappear from the student view")
        boolean active
) {
}

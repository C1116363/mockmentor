package com.learn.interviewmentor.vo.project;

import com.learn.interviewmentor.model.ProjectAccessRequest;
import com.learn.interviewmentor.model.ProjectAccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One student's access request, as they see it and as the admin queue shows it. */
@Schema(description = "A request for contributor access to one live project.")
public record ProjectAccessVo(

        Long id,
        Long projectId,

        @Schema(example = "Leegality eSign gateway")
        String projectName,

        @Schema(description = "The GitHub account that gets - or got - access",
                example = "rahul-sharma")
        String githubUsername,

        String motivation,

        @Schema(description = "What this student was charged. Frozen at request time.",
                example = "7999.00")
        BigDecimal pricePaid,

        @Schema(description = "AWAITING_PAYMENT, SUBMITTED, ACTIVE, REJECTED, CANCELLED, "
                + "EXPIRED or REVOKED")
        ProjectAccessStatus status,

        @Schema(description = "ACTIVE and still inside its window. **This is the field to trust** "
                + "for \"do I have access\" - it checks the date, not just the status.")
        boolean currentlyActive,

        @Schema(description = "Whether the GitHub invite has actually gone through. Can be false "
                + "on an ACTIVE row - the payment is confirmed but nobody has added them yet.")
        boolean collaboratorGranted,

        @Schema(description = "Admin-facing: what the admin still needs to do on GitHub, or what "
                + "went wrong. Null when there is nothing outstanding.")
        String grantError,

        // ---- the repo, only once access is live ----

        @Schema(description = "**Only when access is active.** Null otherwise.",
                example = "leegality/esign-gateway")
        String repoFullName,

        @Schema(description = "**Only when access is active.** Null otherwise.")
        String repoUrl,

        @Schema(description = "**Only when access is active.** Null otherwise.")
        String onboardingUrl,

        @Schema(description = "Who reviews your pull requests", example = "Ananya Rao")
        String leadReviewer,

        // ---- payment + timeline ----

        String upiReference,

        @Schema(description = "true once a screenshot is uploaded. Fetch it from "
                + "/api/projects/access/{id}/screenshot")
        boolean hasScreenshot,

        String rejectionReason,
        String revokedReason,

        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,

        @Schema(description = "Which admin reviewed it")
        String reviewedBy,

        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,

        // ---- context for the admin queue, so it needs no second call ----

        @Schema(example = "Rahul Sharma")
        String studentName,

        @Schema(example = "rahul@example.com")
        String studentEmail
) {

    public static ProjectAccessVo from(ProjectAccessRequest r) {
        // The repo goes out only while access is genuinely live. Not "was
        // approved once" - a revoked or expired row must stop naming a private
        // repository, or revoking access would leave the path behind.
        boolean live = r.isCurrentlyActive();
        var project = r.getProject();

        return new ProjectAccessVo(
                r.getId(),
                project.getId(),
                project.getName(),
                r.getGithubUsername(),
                r.getMotivation(),
                r.getPricePaid(),
                r.getStatus(),
                live,
                r.isCollaboratorGranted(),
                r.getGrantError(),
                live ? project.getRepoFullName() : null,
                live ? project.getRepoUrl() : null,
                live ? project.getOnboardingUrl() : null,
                project.getLeadReviewer() == null ? null : project.getLeadReviewer().getFullName(),
                r.getUpiReference(),
                r.getScreenshotFile() != null,
                r.getRejectionReason(),
                r.getRevokedReason(),
                r.getCreatedAt(),
                r.getSubmittedAt(),
                r.getReviewedAt(),
                r.getReviewedBy() == null ? null : r.getReviewedBy().getFullName(),
                r.getGrantedAt(),
                r.getExpiresAt(),
                r.getRevokedAt(),
                r.getStudent().getFullName(),
                r.getStudent().getEmail()
        );
    }
}

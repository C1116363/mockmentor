package com.learn.interviewmentor.vo.project;

import com.learn.interviewmentor.model.LiveProject;
import com.learn.interviewmentor.model.ProjectDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A live project as shown on a card.
 *
 * <b>The repo is only included when the caller may actually see it.</b> These
 * are private repositories, so `repoFullName`, `repoUrl` and `onboardingUrl` are
 * null for anybody browsing - handing out the path to a private repo tells an
 * attacker exactly what to go after, and it would sit in the JSON of a page
 * anyone logged in can open. {@link #forBrowsing} and {@link #forContributor}
 * are the two shapes; there is deliberately no single `from()` that could be
 * called without deciding which.
 */
@Schema(description = "One of our private codebases a student can pay to contribute to.")
public record LiveProjectVo(

        Long id,

        @Schema(example = "Leegality eSign gateway")
        String name,

        @Schema(example = "Production eSign flows used by 200+ banks")
        String summary,

        String description,

        @Schema(description = "Technologies, split for you", example = "[\"Java\",\"Spring Boot\"]")
        List<String> techStack,

        @Schema(description = "What a contributor would actually pick up")
        List<String> sampleTasks,

        @Schema(description = "BEGINNER, INTERMEDIATE or ADVANCED")
        ProjectDifficulty difficulty,

        @Schema(description = "Ready to print", example = "Some experience needed")
        String difficultyLabel,

        @Schema(example = "7999.00")
        BigDecimal price,

        @Schema(example = "90")
        int accessDurationDays,

        @Schema(description = "Who reviews pull requests here", example = "Ananya Rao")
        String leadReviewer,

        @Schema(description = "Seats in total. Null means no limit.", example = "5")
        Integer maxContributors,

        @Schema(description = "Seats currently taken", example = "3")
        long seatsTaken,

        @Schema(description = "false when the project is full", example = "true")
        boolean seatsAvailable,

        // ---- only populated for someone whose access is active ----

        @Schema(description = "**Only when your access is active.** Null otherwise.",
                example = "leegality/esign-gateway")
        String repoFullName,

        @Schema(description = "**Only when your access is active.** Null otherwise.")
        String repoUrl,

        @Schema(description = "**Only when your access is active.** Null otherwise.")
        String onboardingUrl,

        boolean active,
        int displayOrder,
        LocalDateTime updatedAt
) {

    /** For anyone browsing the catalogue. Repository details withheld. */
    public static LiveProjectVo forBrowsing(LiveProject p, long seatsTaken) {
        return build(p, seatsTaken, false);
    }

    /** For a contributor with active access, and for admins. Repo included. */
    public static LiveProjectVo forContributor(LiveProject p, long seatsTaken) {
        return build(p, seatsTaken, true);
    }

    private static LiveProjectVo build(LiveProject p, long seatsTaken, boolean includeRepo) {
        boolean available = p.getMaxContributors() == null || seatsTaken < p.getMaxContributors();
        return new LiveProjectVo(
                p.getId(),
                p.getName(),
                p.getSummary(),
                p.getDescription(),
                p.getTechStackList(),
                p.getSampleTaskList(),
                p.getDifficulty(),
                p.getDifficulty().getLabel(),
                p.getPrice(),
                p.getAccessDurationDays(),
                p.getLeadReviewer() == null ? null : p.getLeadReviewer().getFullName(),
                p.getMaxContributors(),
                seatsTaken,
                available,
                includeRepo ? p.getRepoFullName() : null,
                includeRepo ? p.getRepoUrl() : null,
                includeRepo ? p.getOnboardingUrl() : null,
                p.isActive(),
                p.getDisplayOrder(),
                p.getUpdatedAt()
        );
    }
}

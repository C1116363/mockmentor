package com.learn.interviewmentor.vo.material;

import com.learn.interviewmentor.model.MaterialAudience;
import com.learn.interviewmentor.model.MaterialKind;
import com.learn.interviewmentor.model.StudyMaterial;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** A piece of study material, as listed for a student or for the admin. */
@Schema(description = "Notes, a PDF, or a link an admin sent to students.")
public record StudyMaterialVo(

        Long id,

        @Schema(example = "Spring Boot revision notes")
        String title,

        String description,

        @Schema(description = "FILE or LINK")
        MaterialKind kind,

        @Schema(description = "Download name, FILE only", example = "spring-notes.pdf")
        String fileName,

        @Schema(description = "FILE only", example = "application/pdf")
        String contentType,

        @Schema(description = "FILE only", example = "482301")
        Long sizeBytes,

        @Schema(description = "LINK only", example = "https://example.com/playlist")
        String linkUrl,

        @Schema(description = "ALL_STUDENTS, SPECIFIC_STUDENT or PLAN_MEMBERS")
        MaterialAudience audience,

        @Schema(description = "Who it was sent to, when audience is SPECIFIC_STUDENT")
        String targetStudentName,

        @Schema(description = "Which plan unlocks it, when audience is PLAN_MEMBERS")
        String targetPlanName,

        @Schema(description = "Human-readable audience, ready to print on a chip",
                example = "Only Rahul Sharma")
        String audienceLabel,

        @Schema(description = "Unpublished material is hidden from students")
        boolean active,

        @Schema(description = "Which admin sent it")
        String uploadedBy,

        LocalDateTime createdAt
) {
    public static StudyMaterialVo from(StudyMaterial m) {
        return new StudyMaterialVo(
                m.getId(),
                m.getTitle(),
                m.getDescription(),
                m.getKind(),
                m.getOriginalName(),
                m.getContentType(),
                m.getSizeBytes(),
                m.getLinkUrl(),
                m.getAudience(),
                m.getTargetStudent() == null ? null : m.getTargetStudent().getFullName(),
                m.getTargetPlan() == null ? null : m.getTargetPlan().getName(),
                audienceLabel(m),
                m.isActive(),
                m.getUploadedBy().getFullName(),
                m.getCreatedAt()
        );
    }

    /**
     * Built here rather than in the UI so the admin list and the student list
     * cannot describe the same row differently.
     */
    private static String audienceLabel(StudyMaterial m) {
        return switch (m.getAudience()) {
            case ALL_STUDENTS -> "All students";
            case SPECIFIC_STUDENT -> m.getTargetStudent() == null
                    ? "One student"
                    : "Only " + m.getTargetStudent().getFullName();
            case PLAN_MEMBERS -> m.getTargetPlan() == null
                    ? "Plan members"
                    : m.getTargetPlan().getName() + " members";
        };
    }
}

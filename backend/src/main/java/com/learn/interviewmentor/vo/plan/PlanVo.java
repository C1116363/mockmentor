package com.learn.interviewmentor.vo.plan;

import com.learn.interviewmentor.model.Plan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A plan as shown on a card, to students and to the public site. */
@Schema(description = "Something a student can buy. Price comes from the database, so an admin "
        + "changing it in the admin panel is visible here immediately.")
public record PlanVo(

        Long id,

        @Schema(example = "Placement Guide")
        String name,

        @Schema(example = "Everything from resume to offer letter")
        String tagline,

        String description,

        @Schema(description = "Bullet points for the card")
        List<String> features,

        @Schema(description = "Rupees", example = "2999.00")
        BigDecimal price,

        @Schema(description = "How long access lasts once payment is verified", example = "90")
        int durationDays,

        @Schema(description = "Retired plans are inactive and hidden from students")
        boolean active,

        int displayOrder,

        @Schema(description = "Draws the 'most popular' ribbon")
        boolean highlighted,

        LocalDateTime updatedAt
) {
    public static PlanVo from(Plan plan) {
        return new PlanVo(
                plan.getId(),
                plan.getName(),
                plan.getTagline(),
                plan.getDescription(),
                plan.getFeatureList(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.isActive(),
                plan.getDisplayOrder(),
                plan.isHighlighted(),
                plan.getUpdatedAt()
        );
    }
}

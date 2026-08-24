package com.learn.interviewmentor.dto.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Admin creating or editing a whole plan. */
@Schema(description = "Create or replace a plan. ADMIN only.")
public record PlanRequestDto(

        @NotBlank(message = "Give the plan a name")
        @Size(max = 120, message = "Name must be 120 characters or fewer")
        @Schema(example = "Placement Guide")
        String name,

        @Size(max = 200, message = "Tagline must be 200 characters or fewer")
        @Schema(example = "Everything from resume to offer letter")
        String tagline,

        @Size(max = 5000, message = "Description must be 5000 characters or fewer")
        String description,

        @Size(max = 5000, message = "Features must be 5000 characters or fewer")
        @Schema(description = "One bullet point per line",
                example = "Resume review by a hiring manager\nMock interviews\nDSA sheet")
        String features,

        // A free plan is legitimate; a negative price is not, and the upper
        // bound stops a slipped decimal point becoming a lakh-rupee card.
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @DecimalMax(value = "999999.99", message = "That price looks like a typo")
        @Schema(example = "2999.00")
        BigDecimal price,

        @Min(value = 1, message = "A plan must last at least a day")
        @Max(value = 3650, message = "A plan cannot last more than 10 years")
        @Schema(example = "90")
        int durationDays,

        @Schema(description = "Lower numbers appear first", example = "1")
        int displayOrder,

        boolean highlighted,

        @Schema(description = "Inactive plans stay in the database but disappear from student view")
        boolean active
) {
}

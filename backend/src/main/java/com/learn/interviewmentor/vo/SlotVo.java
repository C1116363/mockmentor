package com.learn.interviewmentor.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * One hour a mentor offered, as shown to the student.
 *
 * These are not generated. A slot appears in the grid because a verified mentor
 * declared that hour for this kind of session - so `mentorsOffering` is always at
 * least 1, and an empty grid means nobody has put their hand up for that day.
 */
@Schema(description = "A one-hour slot a mentor has offered.")
public record SlotVo(

        @Schema(description = "Start of the slot", example = "2026-09-20T15:00:00")
        LocalDateTime start,

        @Schema(description = "End of the slot - always one hour after the start",
                example = "2026-09-20T16:00:00")
        LocalDateTime end,

        @Schema(description = "Short label for the button", example = "3:00 PM")
        String label,

        @Schema(description = "false when the slot has passed or is fully booked", example = "true")
        boolean available,

        @Schema(description = "Why it is unavailable, or null when it is bookable. "
                + "\"Needs 24 hours' notice\" is different from \"Fully booked\" - one means "
                + "try sooner, the other means try another time.",
                example = "Fully booked")
        String unavailableReason,

        @Schema(description = "How many more students could book this hour", example = "2")
        long remaining,

        @Schema(description = "How many verified mentors offered this hour for this kind of "
                + "session. A slot only exists because at least one did.", example = "3")
        int mentorsOffering
) {
}

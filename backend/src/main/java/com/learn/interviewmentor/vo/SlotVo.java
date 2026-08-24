package com.learn.interviewmentor.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** One bookable 1-hour slot, as offered to the candidate. */
@Schema(description = "A one-hour slot the candidate can pick.")
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

        @Schema(description = "Why it is unavailable, or null when it is bookable",
                example = "Fully booked")
        String unavailableReason
) {
}

package com.learn.interviewmentor.dto.mentor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * A mentor declaring the hours they are free on one day.
 *
 * Bulk on purpose - a mentor thinks "Tuesday afternoon", not "3 PM, then 4 PM,
 * then 5 PM". One request per day keeps the round trips down and lets the server
 * report which hours were rejected and why in a single answer.
 */
@Schema(description = "Declare the hours you are free on one day. MENTOR only.")
public record AvailabilityRequestDto(

        @NotNull(message = "Pick a date")
        @Schema(description = "The day these hours are on", example = "2026-09-22",
                type = "string", format = "date")
        LocalDate date,

        @NotEmpty(message = "Pick at least one hour")
        @Size(max = 24, message = "There are only 24 hours in a day")
        @Schema(description = "Start hours in 24-hour clock. 15 means 3 PM - 4 PM.",
                example = "[15, 16, 17]")
        List<Integer> hours,

        @Schema(description = "Will you take mock interviews in these hours?", example = "true")
        boolean forInterviews,

        @Schema(description = "Will you take mentoring discussions in these hours?", example = "true")
        boolean forMentoring,

        @Size(max = 300)
        @Schema(description = "Anything the admin should know",
                example = "Prefer backend topics this week")
        String note
) {
}

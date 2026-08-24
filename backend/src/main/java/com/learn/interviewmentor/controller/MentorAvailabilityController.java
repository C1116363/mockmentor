package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.mentor.AvailabilityRequestDto;
import com.learn.interviewmentor.facade.MentorFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.vo.mentor.MentorAvailabilityVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A mentor declaring the hours they are free.
 *
 * <b>This is where the slot grid comes from.</b> Students are shown the union of
 * what mentors declare - nothing is generated - so an hour nobody offers is an
 * hour nobody can book. Before this existed the grid was a fixed 9-to-9 with
 * capacity set to "how many verified mentors exist", which sold 7 AM Sunday slots
 * that nobody had agreed to take.
 *
 * Its own controller rather than more methods on MentorProfileController: that one
 * is mapped to /api/mentor/profile, and availability is a separate feature with
 * its own table, its own service and its own lifecycle.
 */
@RestController
@RequestMapping("/api/mentor/availability")
@Tag(name = "3. Mentor availability",
        description = "**MENTOR only.** Declare the hours you are free. This is what students "
                + "see - the slot grid is the union of these, so an hour nobody offers is an "
                + "hour nobody can book. Admins read everyone's under /api/admin/availability.")
public class MentorAvailabilityController {

    private final MentorFacade mentorFacade;

    public MentorAvailabilityController(MentorFacade mentorFacade) {
        this.mentorFacade = mentorFacade;
    }

    @PostMapping
    @Operation(
            summary = "Declare the hours you are free",
            description = """
                    Send a date and the hours in 24-hour clock:

                    ```json
                    { "date": "2026-09-22", "hours": [15, 16, 17],
                      "forInterviews": true, "forMentoring": true }
                    ```

                    Tick what you will actually take. A mentor happy to run a mock interview
                    but not a career discussion says so here, and the two grids differ
                    accordingly.

                    **Partial success is normal.** Hours already booked, outside 9 AM - 9 PM, or
                    less than 24 hours away are skipped rather than failing the whole request -
                    the response message names each one and why. A mentor ticking a whole
                    afternoon should not lose the lot because one hour is too close.

                    Re-sending an hour you already offered **updates** it rather than
                    duplicating it, so changing your mind about mentoring vs interviews is the
                    same call.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Hours offered. Check the message for any that were skipped."),
            @ApiResponse(responseCode = "400",
                    description = "No hours, neither session kind ticked, or beyond 30 days",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Your profile is not verified yet",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<MentorAvailabilityVo>> declare(
            @Valid @RequestBody AvailabilityRequestDto request,
            @CurrentUser User mentor) {
        return mentorFacade.declareAvailability(request, mentor);
    }

    @GetMapping
    @Operation(
            summary = "My upcoming hours",
            description = "From now onwards - past hours are history, and this list is a to-do. "
                    + "`status` is OPEN, BOOKED (an admin mapped a student onto it, and you can "
                    + "see who) or WITHDRAWN.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "My declared hours"))
    public ApiResult<List<MentorAvailabilityVo>> mine(@CurrentUser User mentor) {
        return mentorFacade.myAvailability(mentor);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Take an hour back",
            description = "Only while nobody is booked into it, and only while it is still more "
                    + "than 24 hours away. Inside that window a student may already be counting "
                    + "on it and there is no time left to reassign - contact an admin instead of "
                    + "leaving them with a session and no interviewer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawn"),
            @ApiResponse(responseCode = "403", description = "Not your availability",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409",
                    description = "Already booked, or inside the 24-hour window",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorAvailabilityVo> withdraw(
            @Parameter(description = "Availability id", example = "1") @PathVariable Long id,
            @CurrentUser User mentor) {
        return mentorFacade.withdrawAvailability(id, mentor);
    }
}

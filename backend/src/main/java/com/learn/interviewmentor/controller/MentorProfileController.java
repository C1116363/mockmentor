package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.dto.mentor.MentorProfileDto;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequest;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.service.MentorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A mentor's own onboarding. Locked to MENTOR - a candidate has no profile to
 * fill in, and an admin reviews profiles through /api/admin instead.
 */
@RestController
@RequestMapping("/api/mentor/profile")
@PreAuthorize("hasRole('MENTOR')")
@Tag(name = "3. Mentor onboarding",
        description = "**MENTOR only.** Fill in your profile, then wait for an admin to verify it. "
                + "You cannot see the interview queue until you are APPROVED.")
public class MentorProfileController {

    private final MentorProfileService profileService;

    public MentorProfileController(MentorProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(
            summary = "My profile and verification status",
            description = """
                    The frontend calls this right after a mentor logs in and uses
                    `verificationStatus` to decide what to show:

                    - `INCOMPLETE` → the profile form
                    - `PENDING` → "under verification" screen
                    - `REJECTED` → the reason, plus the form again to fix and resubmit
                    - `APPROVED` → the mentor dashboard

                    Aadhaar and the bank account come back masked to the last 4 digits.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Your profile"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not a MENTOR",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public MentorProfileDto myProfile(@CurrentUser User mentor) {
        return profileService.myProfile(mentor);
    }

    @PutMapping
    @Operation(
            summary = "Submit the profile for verification",
            description = """
                    Saves the form and moves you to `PENDING`. Use the same call to resubmit
                    after a rejection.

                    Once you are `APPROVED` this is locked - a verified mentor should not be
                    able to quietly change their bank details afterwards.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submitted, now PENDING"),
            @ApiResponse(responseCode = "400",
                    description = "Already approved or already under review, or validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not a MENTOR",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public MentorProfileDto submit(@Valid @RequestBody MentorProfileRequest dto, @CurrentUser User mentor) {
        return profileService.submit(dto, mentor);
    }
}

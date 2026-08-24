package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.facade.MentorFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only browsing of mentors. Creating a mentor is signup, so it lives in
 * AuthController instead.
 */
@RestController
@RequestMapping("/api/mentors")
@Tag(name = "3. Mentors", description = "Browse the mentor directory. Any logged-in user can read this.")
public class MentorController {

    private final MentorFacade mentorFacade;

    public MentorController(MentorFacade mentorFacade) {
        this.mentorFacade = mentorFacade;
    }

    @GetMapping
    @Operation(
            summary = "List all mentors",
            description = "Most experienced first. To *create* a mentor use "
                    + "`POST /api/auth/signup/mentor` - registering is signing up.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The mentor directory"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<MentorVo>> listMentors() {
        return mentorFacade.approvedMentors();
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get one mentor",
            description = "Looked up by the mentor's **user id**, not their profile id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The mentor"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "That user has no mentor profile",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorVo> getMentor(
            @Parameter(description = "The mentor's user id", example = "2") @PathVariable Long userId) {
        return mentorFacade.mentor(userId);
    }
}

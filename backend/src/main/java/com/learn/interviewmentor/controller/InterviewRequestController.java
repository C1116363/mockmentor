package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.facade.SessionFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@Tag(name = "2. Interview requests",
        description = "The core workflow: a student raises a request, a mentor accepts it, "
                + "then completes it with feedback.")
public class InterviewRequestController {

    private final SessionFacade sessionFacade;

    public InterviewRequestController(SessionFacade sessionFacade) {
        this.sessionFacade = sessionFacade;
    }

    @PostMapping
    @Operation(
            summary = "Raise an interview request",
            description = """
                    **Role required: STUDENT**

                    Notice there is no name or email in the body. The server reads the student
                    off the JWT. If the client could send them, anyone could raise a request in
                    somebody else's name.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created with status PENDING"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. date in the past)",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not a STUDENT",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<InterviewRequestVo> createRequest(@Valid @RequestBody CreateRequestDto dto,
                                                             @CurrentUser User student) {
        return sessionFacade.book(dto, student);
    }

    @GetMapping("/mine")
    @Operation(
            summary = "My requests (student)",
            description = "Every request the logged-in student has raised, newest first. "
                    + "There is no email parameter - you can only ever see your own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Your requests"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<InterviewRequestVo>> myRequests(@CurrentUser User student) {
        return sessionFacade.mine(student);
    }

    @GetMapping("/pending")
    @Operation(
            summary = "The open queue (mentor)",
            description = "**Role required: MENTOR, and you must be APPROVED by an admin.**\n\nEverything still PENDING, oldest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unclaimed requests"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not a MENTOR",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<InterviewRequestVo>> pendingRequests(@CurrentUser User mentor) {
        return sessionFacade.openQueue(mentor);
    }

    @GetMapping("/assigned")
    @Operation(
            summary = "Interviews I accepted (mentor)",
            description = "Everything the logged-in mentor has picked up, soonest slot first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Your accepted interviews"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<InterviewRequestVo>> myInterviews(@CurrentUser User mentor) {
        return sessionFacade.assignedTo(mentor);
    }

    @PatchMapping("/{id}/accept")
    @Operation(
            summary = "Accept a request and schedule it",
            description = """
                    **Role required: MENTOR**

                    Moves the request `PENDING -> SCHEDULED`. There is no `mentorId` in the body:
                    the mentor is whoever holds the token.

                    A request can only be accepted while it is still PENDING - accepting one
                    twice returns 400.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scheduled"),
            @ApiResponse(responseCode = "400", description = "Already accepted, or the slot is not in the future",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not a MENTOR",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No request with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<InterviewRequestVo> acceptRequest(
            @Parameter(description = "Id of the interview request", example = "1") @PathVariable Long id,
            @Valid @RequestBody AcceptRequestDto dto,
            @CurrentUser User mentor) {
        return sessionFacade.accept(id, dto, mentor);
    }

    @PatchMapping("/{id}/complete")
    @Operation(
            summary = "Close an interview with feedback",
            description = """
                    **Role required: MENTOR, and it must be _your_ interview.**

                    Moves `SCHEDULED -> COMPLETED`. Being a mentor is not enough - a mentor
                    trying to complete somebody else's interview gets 403. That ownership check
                    lives in the service, not in the URL rules.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completed"),
            @ApiResponse(responseCode = "400", description = "The request is not SCHEDULED",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not a mentor, or another mentor accepted it",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No request with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<InterviewRequestVo> completeRequest(
            @Parameter(description = "Id of the interview request", example = "1") @PathVariable Long id,
            @Valid @RequestBody CompleteRequestDto dto,
            @CurrentUser User mentor) {
        return sessionFacade.complete(id, dto, mentor);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel a request",
            description = """
                    Allowed for the student who raised it, the mentor who accepted it, or any ADMIN.
                    Anyone else gets 403. A COMPLETED interview cannot be cancelled.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled"),
            @ApiResponse(responseCode = "400", description = "Already completed",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not yours to cancel",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No request with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<InterviewRequestVo> cancelRequest(
            @Parameter(description = "Id of the interview request", example = "1") @PathVariable Long id,
            @CurrentUser User actor) {
        return sessionFacade.cancel(id, actor);
    }
}

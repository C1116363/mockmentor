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
import com.learn.interviewmentor.model.InterviewRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
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

    // ---------------- the candidate's CV ----------------

    @PostMapping(value = "/{id}/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Attach your CV to a booking",
            description = """
                    Optional, and separate from creating the booking on purpose: if the
                    upload fails you still have a booking rather than losing the slot over
                    a file.

                    PDF or Word, up to 5 MB. **PDF is best** - it looks the same on the
                    interviewer's machine as it does on yours.

                    Send it again to replace it; the old one is deleted. Allowed right up
                    until the session is completed or cancelled, because people find a typo
                    on their CV the evening before and the interviewer wants the version
                    that will actually be in front of them.

                    The CV is attached to **this booking**, not to your account - a CV
                    changes between March and August, and an interviewer needs the one that
                    was current for the session they are running.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attached"),
            @ApiResponse(responseCode = "400",
                    description = "Not a PDF or Word file, over 5 MB, or named .pdf but not a PDF",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not your booking",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "The session is already finished",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<InterviewRequestVo> attachCv(
            @Parameter(description = "Booking id", example = "1") @PathVariable Long id,
            @Parameter(description = "Your CV as a PDF or Word document") @RequestParam MultipartFile cv,
            @CurrentUser User student) {
        return sessionFacade.attachCv(id, cv, student);
    }

    @GetMapping("/{id}/cv")
    @Operation(
            summary = "Download the candidate's CV",
            description = """
                    **Only the candidate, the mentor assigned to this session, and admins.**

                    Note who is deliberately excluded: a mentor browsing the open queue. A CV
                    carries a phone number and an address, and letting every approved mentor
                    read every candidate's CV before deciding whether to accept would turn a
                    booking list into a CV database - which is not what anybody uploaded it
                    for. Accept the session first.

                    Served as an attachment with `nosniff`, so an uploaded file can never
                    render as a page in our own origin.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The CV"),
            @ApiResponse(responseCode = "403",
                    description = "You are not the candidate or the assigned mentor",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No CV attached, or the file is gone",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id, @CurrentUser User caller) {
        InterviewRequest request = sessionFacade.cvFor(id, caller);
        Path path = sessionFacade.cvPath(request);

        String type = request.getCvContentType() == null
                ? "application/octet-stream" : request.getCvContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                // The candidate's own filename comes back as the download name. It
                // was stripped of quotes, backslashes and newlines on the way in,
                // so it cannot break out of the header here.
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.getCvOriginalName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}

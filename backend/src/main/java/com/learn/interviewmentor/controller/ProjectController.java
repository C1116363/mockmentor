package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.project.ProjectAccessApplicationDto;
import com.learn.interviewmentor.facade.ProjectFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.project.LiveProjectVo;
import com.learn.interviewmentor.vo.project.ProjectAccessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Live projects as a student sees them: browse, request access, pay.
 *
 * These are our own private repositories, not open source. Contributing to them
 * means real pull requests reviewed by a senior engineer, on code that runs -
 * which is the thing a beginner cannot get from a public repo nobody reviews.
 *
 * <b>Note the repository path is withheld until access is live.</b> Every
 * response here has `repoFullName`, `repoUrl` and `onboardingUrl` as null unless
 * you hold active access to that project. Naming a private repo to somebody who
 * cannot open it tells an attacker exactly what to aim at, for no benefit.
 */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "8. Live projects",
        description = "Our private codebases. Pay for contributor access, raise pull requests, "
                + "get them reviewed by a senior engineer. The repository path is only returned "
                + "once your access is active.")
public class ProjectController {

    private final ProjectFacade projectFacade;

    public ProjectController(ProjectFacade projectFacade) {
        this.projectFacade = projectFacade;
    }

    // ---------------- browsing ----------------

    @GetMapping
    @Operation(
            summary = "Projects open for contributors",
            description = "Active projects, in the order an admin arranged them. `seatsTaken` and "
                    + "`seatsAvailable` tell you whether there is room - a real repo has a real "
                    + "review budget, so most projects cap how many contributors hold access at "
                    + "once.\n\n"
                    + "The repository fields are populated only for projects you already have "
                    + "access to.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Open projects"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<LiveProjectVo>> projects(@CurrentUser User caller) {
        return projectFacade.openProjects(caller);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The project"),
            @ApiResponse(responseCode = "404", description = "No such project",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<LiveProjectVo> project(
            @Parameter(description = "Project id", example = "1") @PathVariable Long id,
            @CurrentUser User caller) {
        return projectFacade.project(id, caller);
    }

    // ---------------- requesting access ----------------

    @PostMapping("/{id}/request-access")
    @Operation(
            summary = "Request contributor access",
            description = "Creates a request in AWAITING_PAYMENT. Next step is to fetch the "
                    + "payment instructions and send proof.\n\n"
                    + "**Your GitHub username is required here**, not later - it is what we add to "
                    + "the repository, and collecting it up front means an admin with a verified "
                    + "payment is never waiting on an email before they can grant anything.\n\n"
                    + "Safe to call twice: if you already have a request in progress you get that "
                    + "one back, with the username updated to what you just sent.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created"),
            @ApiResponse(responseCode = "400", description = "Invalid GitHub username",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Only students can request access",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No such project",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409",
                    description = "You already have access, the project is full, or it is closed",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> requestAccess(
            @Parameter(description = "Project id", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProjectAccessApplicationDto request,
            @CurrentUser User student) {
        return projectFacade.apply(id, request, student);
    }

    @GetMapping("/access/mine")
    @Operation(
            summary = "My project access",
            description = "Every request I have made, newest first. `currentlyActive` is the field "
                    + "to trust for \"can I push\" - it checks the expiry date, not just the "
                    + "status. `collaboratorGranted` tells you whether the GitHub invite has "
                    + "actually gone out yet.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "My requests"))
    public ApiResult<List<ProjectAccessVo>> myAccess(@CurrentUser User student) {
        return projectFacade.myAccess(student);
    }

    @GetMapping("/access/{id}/instructions")
    @Operation(
            summary = "Where to pay, and how much",
            description = "The amount comes from the request, which froze the price when it was "
                    + "created. It is never read from the client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment instructions"),
            @ApiResponse(responseCode = "403", description = "Not your request",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentInstructionsVo> instructions(@PathVariable Long id,
                                                         @CurrentUser User caller) {
        return projectFacade.paymentInstructions(id, caller);
    }

    @PostMapping(value = "/access/{id}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Send proof of payment",
            description = "Multipart: the UTR plus a screenshot from your UPI app. Use the same "
                    + "call to resubmit after a rejection. JPG, PNG or WebP up to 5 MB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proof received, now SUBMITTED"),
            @ApiResponse(responseCode = "400", description = "Missing UTR, or the file was rejected",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not your request",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "Already paid, or already under review",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> submitProof(
            @PathVariable Long id,
            @Parameter(description = "UPI transaction / UTR number") @RequestParam String upiReference,
            @Parameter(description = "Screenshot from the UPI app") @RequestParam MultipartFile screenshot,
            @CurrentUser User student) {
        return projectFacade.submitProof(id, upiReference, screenshot, student);
    }

    @PatchMapping("/access/{id}/github-username")
    @Operation(
            summary = "Fix a mistyped GitHub username",
            description = "Only before access has been granted. Once the old handle is already a "
                    + "collaborator, changing this silently would leave that access in place with "
                    + "nothing pointing at it - so it returns 409 and an admin has to do it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "409", description = "Access is already granted to the old handle",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> changeGithubUsername(
            @PathVariable Long id,
            @Parameter(description = "The corrected GitHub username") @RequestParam String githubUsername,
            @CurrentUser User student) {
        return projectFacade.changeGithubUsername(id, githubUsername, student);
    }

    @PatchMapping("/access/{id}/cancel")
    @Operation(summary = "Withdraw a request before paying")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled"),
            @ApiResponse(responseCode = "409", description = "Access is already live",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> cancel(@PathVariable Long id, @CurrentUser User student) {
        return projectFacade.cancel(id, student);
    }

    @GetMapping("/access/{id}/screenshot")
    @Operation(
            summary = "Download the payment screenshot",
            description = "Only the student who uploaded it and admins can fetch this.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The image"),
            @ApiResponse(responseCode = "403", description = "Not yours to view",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No screenshot uploaded",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ResponseEntity<Resource> screenshot(@PathVariable Long id, @CurrentUser User caller) {
        Path path = projectFacade.screenshotPath(id, caller);
        String type = projectFacade.screenshotContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type == null ? "application/octet-stream" : type))
                // attachment, not inline: never let an uploaded file render as a
                // page in our own origin.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}

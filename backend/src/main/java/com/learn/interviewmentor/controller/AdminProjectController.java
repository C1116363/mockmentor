package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.mentor.RejectProfileRequestDto;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.project.LiveProjectRequestDto;
import com.learn.interviewmentor.facade.ProjectFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The admin side of live projects: the catalogue, and who gets access.
 *
 * <h2>The three queues, and why there are three</h2>
 * <ul>
 *   <li><b>/access/pending</b> - paid, needs the UTR checked. Money.</li>
 *   <li><b>/access/awaiting-invite</b> - payment confirmed, but nobody has added
 *       them on GitHub yet. <b>These people have paid and cannot see the code.</b>
 *       Invisible if approving and inviting were one flag.</li>
 *   <li><b>/access/past-expiry</b> - access has run out but they are still a
 *       collaborator. Access outliving what was paid for is the quiet failure
 *       worth surfacing, because nothing sweeps it automatically.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/projects")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "9. Admin · live projects",
        description = "**ADMIN only.** Manage the project catalogue and contributor access. "
                + "Granting access has an effect on GitHub, so approving and actually inviting "
                + "are tracked separately.")
public class AdminProjectController {

    private final ProjectFacade projectFacade;

    public AdminProjectController(ProjectFacade projectFacade) {
        this.projectFacade = projectFacade;
    }

    // ---------------- the catalogue ----------------

    @GetMapping
    @Operation(
            summary = "Every project",
            description = "Closed projects included - they are inactive, not deleted, because "
                    + "access rows point at them.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All projects"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<LiveProjectVo>> projects() {
        return projectFacade.allProjects();
    }

    @PostMapping
    @Operation(
            summary = "Add a project",
            description = "The repo is stored as owner + name, which is what the GitHub API needs. "
                    + "Both are validated against GitHub's own naming rules here, because a "
                    + "malformed one means the invite 404s later - after somebody has paid.\n\n"
                    + "`maxContributors` is worth setting. One senior engineer cannot "
                    + "meaningfully review thirty newcomers at once, and selling access past that "
                    + "sells something you cannot deliver.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "That name, or that repo, is already used",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<LiveProjectVo> create(@Valid @RequestBody LiveProjectRequestDto request) {
        return projectFacade.create(request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Replace a project",
            description = "Every field is overwritten, so send the whole project back.\n\n"
                    + "Changing the **repository** is refused while contributors still hold "
                    + "access: their rows would silently start claiming access to a codebase "
                    + "nobody granted them.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such project",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409",
                    description = "Name taken, or the repo was changed while contributors have access",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<LiveProjectVo> update(@PathVariable Long id,
                                           @Valid @RequestBody LiveProjectRequestDto request) {
        return projectFacade.update(id, request);
    }

    @PatchMapping("/{id}/price")
    @Operation(
            summary = "Change just the price",
            description = "Live for the next request immediately. Anyone who already requested "
                    + "access keeps the price they were quoted - the request copies it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New price"),
            @ApiResponse(responseCode = "400", description = "Negative, or a typo-sized number",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<LiveProjectVo> changePrice(@PathVariable Long id,
                                                @Valid @RequestBody PlanPriceRequestDto request) {
        return projectFacade.changePrice(id, request);
    }

    @PatchMapping("/{id}/active")
    @Operation(
            summary = "Open or close to new contributors",
            description = "Closing does **not** revoke anybody. People mid-contribution keep "
                    + "access until it expires - taking code access away because a project "
                    + "stopped selling would be a surprise nobody agreed to.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Updated"))
    public ApiResult<LiveProjectVo> setActive(
            @PathVariable Long id,
            @Parameter(description = "true to open, false to close") @RequestParam boolean active) {
        return projectFacade.setActive(id, active);
    }

    @GetMapping("/{id}/contributors")
    @Operation(summary = "Who currently has access to this project")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Active contributors"))
    public ApiResult<List<ProjectAccessVo>> contributors(@PathVariable Long id) {
        return projectFacade.contributorsOn(id);
    }

    // ---------------- access requests ----------------

    @GetMapping("/access/pending")
    @Operation(
            summary = "Access payments to verify",
            description = "Oldest submission first. Check the UTR against your bank, then approve.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Awaiting review"))
    public ApiResult<List<ProjectAccessVo>> pending() {
        return projectFacade.pendingAccess();
    }

    @GetMapping("/access/awaiting-invite")
    @Operation(
            summary = "Paid, but not yet added on GitHub",
            description = "**The queue that matters most.** These people have paid, their access "
                    + "is active in our system, and they still cannot open the repository. Each "
                    + "row carries the username and the repo settings URL to act on.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Awaiting the GitHub invite"))
    public ApiResult<List<ProjectAccessVo>> awaitingInvite() {
        return projectFacade.awaitingInvite();
    }

    @GetMapping("/access/past-expiry")
    @Operation(
            summary = "Access that has run out but is still granted",
            description = "Nothing sweeps this automatically, so these contributors are still on "
                    + "the repo after their window closed. Revoke them to close it off.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Past expiry"))
    public ApiResult<List<ProjectAccessVo>> pastExpiry() {
        return projectFacade.pastExpiry();
    }

    @GetMapping("/access")
    @Operation(summary = "Every access request", description = "All statuses, newest first.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "All requests"))
    public ApiResult<List<ProjectAccessVo>> allAccess() {
        return projectFacade.allAccess();
    }

    @PatchMapping("/access/{id}/approve")
    @Operation(
            summary = "Confirm the payment and start access",
            description = "Starts the access window and attempts the GitHub grant. With the "
                    + "default manual provider that means you still have to add them yourself - "
                    + "the response message carries the link, and the row stays in the "
                    + "awaiting-invite queue until you confirm it.\n\n"
                    + "Seats are re-checked here: days can pass between a request and this, and "
                    + "the last seat can go in between.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approved; check the message for the GitHub step"),
            @ApiResponse(responseCode = "409", description = "Not SUBMITTED, or the project is now full",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> approve(@PathVariable Long id, @CurrentUser User admin) {
        return projectFacade.approve(id, admin);
    }

    @PatchMapping("/access/{id}/confirm-invite")
    @Operation(
            summary = "Mark the collaborator as added",
            description = "Click this once you have actually added them on GitHub. It clears the "
                    + "row out of the awaiting-invite queue and tells the student their access is "
                    + "live.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Confirmed"),
            @ApiResponse(responseCode = "409", description = "That access is not active",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> confirmInvite(@PathVariable Long id,
                                                    @CurrentUser User admin) {
        return projectFacade.confirmCollaboratorAdded(id, admin);
    }

    @PatchMapping("/access/{id}/reject")
    @Operation(summary = "Reject the payment",
            description = "The student sees the reason and can send new proof.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "No reason given",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> reject(@PathVariable Long id,
                                             @Valid @RequestBody RejectProfileRequestDto request,
                                             @CurrentUser User admin) {
        return projectFacade.reject(id, request.reason(), admin);
    }

    @PatchMapping("/access/{id}/revoke")
    @Operation(
            summary = "Take access away",
            description = "For misuse, or for access that has expired. The student sees the "
                    + "reason. **Remember to remove them on GitHub too** - the response message "
                    + "reminds you, because our row going REVOKED does not by itself take push "
                    + "access away.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revoked"),
            @ApiResponse(responseCode = "400", description = "No reason given",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "That access is not ACTIVE",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<ProjectAccessVo> revoke(@PathVariable Long id,
                                             @Valid @RequestBody RejectProfileRequestDto request,
                                             @CurrentUser User admin) {
        return projectFacade.revoke(id, request.reason(), admin);
    }
}

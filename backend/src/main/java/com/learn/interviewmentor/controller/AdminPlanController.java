package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.dto.material.MaterialLinkRequestDto;
import com.learn.interviewmentor.vo.material.StudyMaterialVo;
import com.learn.interviewmentor.dto.payment.RejectPaymentRequestDto;
import com.learn.interviewmentor.vo.plan.PlanVo;
import com.learn.interviewmentor.vo.plan.PlanEnrollmentVo;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.plan.PlanRequestDto;
import com.learn.interviewmentor.facade.PlanFacade;
import com.learn.interviewmentor.facade.StudyMaterialFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The admin side of plans and study material.
 *
 * Under /api/admin, so SecurityConfig's URL rule already locks it to ADMIN. The
 * class-level @PreAuthorize is belt and braces - the same arrangement as
 * {@link AdminController}, which this deliberately does not get folded into:
 * that class is already long, and Swagger reads better with plans and material
 * as their own tag.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "7. Admin · plans & material",
        description = "**ADMIN only.** Set plan prices, confirm plan payments, and send study "
                + "material to everyone, to one student, or to a plan's members.")
public class AdminPlanController {

    private final PlanFacade planFacade;
    private final StudyMaterialFacade materialFacade;

    public AdminPlanController(PlanFacade planFacade, StudyMaterialFacade materialFacade) {
        this.planFacade = planFacade;
        this.materialFacade = materialFacade;
    }

    // ---------------- plans ----------------

    @GetMapping("/plans")
    @Operation(
            summary = "Every plan",
            description = "Retired plans included - they are inactive, not deleted, so they can be "
                    + "switched back on.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All plans"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<PlanVo>> plans() {
        return planFacade.allPlans();
    }

    @PostMapping("/plans")
    @Operation(summary = "Create a plan",
            description = "It appears to students immediately unless you create it inactive.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "A plan with that name already exists",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanVo> create(@Valid @RequestBody PlanRequestDto request) {
        return planFacade.create(request);
    }

    @PutMapping("/plans/{id}")
    @Operation(summary = "Replace a plan",
            description = "Every field is overwritten, so send the whole plan back.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "That name belongs to another plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanVo> update(@PathVariable Long id, @Valid @RequestBody PlanRequestDto request) {
        return planFacade.update(id, request);
    }

    @PatchMapping("/plans/{id}/price")
    @Operation(
            summary = "Change just the price",
            description = "The common case, and its own endpoint so a half-filled edit form can "
                    + "never blank out a description.\n\n"
                    + "**Students see the new price on their next page load.** Nobody's completed "
                    + "purchase changes: an enrollment copies the price when it is created, so "
                    + "what somebody already paid stays what they paid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New price, live immediately"),
            @ApiResponse(responseCode = "400", description = "Negative price, or one that looks like a typo",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No such plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanVo> changePrice(
            @Parameter(description = "Plan id", example = "1") @PathVariable Long id,
            @Valid @RequestBody PlanPriceRequestDto request) {
        return planFacade.changePrice(id, request);
    }

    @PatchMapping("/plans/{id}/active")
    @Operation(
            summary = "Retire or revive a plan",
            description = "There is no delete. Enrollments point at the row, so retiring is the "
                    + "only safe way to take a plan off sale.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanVo> setPlanActive(
            @PathVariable Long id,
            @Parameter(description = "true to put it on sale, false to retire it") @RequestParam boolean active) {
        return planFacade.setActive(id, active);
    }

    // ---------------- plan payments ----------------

    @GetMapping("/plan-enrollments/pending")
    @Operation(
            summary = "Plan payments to verify",
            description = "Students who sent a UPI reference and screenshot for a plan. Oldest "
                    + "submission first. Confirming is what grants them access.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Awaiting review"))
    public ApiResult<List<PlanEnrollmentVo>> pendingEnrollments() {
        return planFacade.pendingEnrollments();
    }

    @GetMapping("/plan-enrollments")
    @Operation(summary = "Every plan purchase", description = "All statuses, newest first.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "All purchases"))
    public ApiResult<List<PlanEnrollmentVo>> allEnrollments() {
        return planFacade.allEnrollments();
    }

    @PatchMapping("/plan-enrollments/{id}/activate")
    @Operation(
            summary = "Confirm the money arrived",
            description = "Check the UTR against your bank first. This starts the access window, "
                    + "which runs for the plan's duration from now.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Now ACTIVE"),
            @ApiResponse(responseCode = "409", description = "Not in SUBMITTED",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanEnrollmentVo> activate(@PathVariable Long id, @CurrentUser User admin) {
        return planFacade.activateEnrollment(id, admin);
    }

    @PatchMapping("/plan-enrollments/{id}/reject")
    @Operation(summary = "Reject the payment",
            description = "The student sees the reason and can send new proof.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "No reason given",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "Not in SUBMITTED",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanEnrollmentVo> rejectEnrollment(@PathVariable Long id,
                                              @Valid @RequestBody RejectPaymentRequestDto request,
                                              @CurrentUser User admin) {
        return planFacade.rejectEnrollment(id, request.reason(), admin);
    }

    // ---------------- study material ----------------

    @GetMapping("/materials")
    @Operation(summary = "Everything ever sent",
            description = "Hidden material included, newest first.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "All material"))
    public ApiResult<List<StudyMaterialVo>> materials() {
        return materialFacade.all();
    }

    @PostMapping(value = "/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file and send it",
            description = "Multipart: a title, an optional description, and the file.\n\n"
                    + "**Audience** — send neither id for every student; send `targetStudentId` "
                    + "for one student only; send `targetPlanId` to reach whoever currently holds "
                    + "that plan. Sending both is a 400: a row has exactly one audience.\n\n"
                    + "PDF, images, ZIP and Office or text documents, up to 25 MB. HTML and SVG "
                    + "are refused because they can carry scripts, and a script served back to a "
                    + "logged-in student from our own origin is stored XSS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sent"),
            @ApiResponse(responseCode = "400",
                    description = "No title, a rejected file type, or both audience ids at once",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No such student or plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "413", description = "File too large",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<StudyMaterialVo> uploadMaterial(
            @Parameter(description = "Title students see") @RequestParam String title,
            @Parameter(description = "Optional note") @RequestParam(required = false) String description,
            @Parameter(description = "The file") @RequestParam MultipartFile file,
            @Parameter(description = "Send to this one student only") @RequestParam(required = false) Long targetStudentId,
            @Parameter(description = "Send to holders of this plan only") @RequestParam(required = false) Long targetPlanId,
            @CurrentUser User admin) {
        return materialFacade.upload(title, description, file, targetStudentId, targetPlanId, admin);
    }

    @PostMapping("/materials/link")
    @Operation(
            summary = "Share a link instead of a file",
            description = "Nothing is stored on disk. Only http and https links are accepted - a "
                    + "`javascript:` or `data:` \"link\" rendered on a student's dashboard would "
                    + "be XSS handed out by an admin.\n\n"
                    + "Audience works exactly as it does for an upload.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sent"),
            @ApiResponse(responseCode = "400", description = "Validation failed, or both audience ids at once",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<StudyMaterialVo> shareLink(
            @Valid @RequestBody MaterialLinkRequestDto request,
            @Parameter(description = "Send to this one student only") @RequestParam(required = false) Long targetStudentId,
            @Parameter(description = "Send to holders of this plan only") @RequestParam(required = false) Long targetPlanId,
            @CurrentUser User admin) {
        return materialFacade.shareLink(request, targetStudentId, targetPlanId, admin);
    }

    @PatchMapping("/materials/{id}/active")
    @Operation(
            summary = "Publish or hide",
            description = "Hiding takes it off every student's list without destroying the file or "
                    + "the record of who it went to. There is no delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "No such material",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<StudyMaterialVo> setMaterialActive(
            @PathVariable Long id,
            @Parameter(description = "true to publish, false to hide") @RequestParam boolean active) {
        return materialFacade.setActive(id, active);
    }
}

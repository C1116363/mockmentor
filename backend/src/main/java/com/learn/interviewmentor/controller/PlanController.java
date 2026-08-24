package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.plan.PlanVo;
import com.learn.interviewmentor.vo.plan.PlanEnrollmentVo;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.facade.PlanFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Plans as a student sees them: browse the price list, buy one, pay for it.
 *
 * Prices are whatever an admin last set in the admin panel - they are read from
 * the database on every call, so a change is live immediately with no redeploy.
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "5. Plans",
        description = "Study plans a student can buy - Placement Guide, technology tracks, and so "
                + "on. Prices are set by an admin and read live from the database. Buying one uses "
                + "the same manual UPI flow as booking an interview.")
public class PlanController {

    private final PlanFacade planFacade;

    public PlanController(PlanFacade planFacade) {
        this.planFacade = planFacade;
    }

    @GetMapping
    @Operation(
            summary = "The plans on sale",
            description = "Active plans only, in the order the admin arranged them. The price here "
                    + "is the live one - if an admin changes it, the next call returns the new "
                    + "number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plans on sale"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<PlanVo>> plans() {
        return planFacade.activePlans();
    }

    @GetMapping("/{id}")
    @Operation(summary = "One plan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The plan"),
            @ApiResponse(responseCode = "404", description = "No such plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanVo> plan(@Parameter(description = "Plan id", example = "1") @PathVariable Long id) {
        return planFacade.plan(id);
    }

    @PostMapping("/{id}/enroll")
    @Operation(
            summary = "Buy this plan",
            description = "Creates a purchase in AWAITING_PAYMENT and returns it. Next step is to "
                    + "fetch the payment instructions and send proof.\n\n"
                    + "Safe to call twice: if you already have a purchase of this plan in progress "
                    + "you get that same one back rather than a second row. The price is copied "
                    + "from the plan at this moment, so a later price change never alters what you "
                    + "were charged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase started (or the one already in progress)"),
            @ApiResponse(responseCode = "403", description = "Only students can buy a plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No such plan",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "You already hold this plan, or it has been retired",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanEnrollmentVo> enroll(
            @Parameter(description = "Plan id", example = "1") @PathVariable Long id,
            @CurrentUser User student) {
        return planFacade.enroll(id, student);
    }

    @GetMapping("/enrollments/mine")
    @Operation(
            summary = "My plans",
            description = "Every plan I have bought or started buying, newest first. "
                    + "`currentlyActive` is the field to trust for \"do I have access\" - it "
                    + "checks the expiry date, not just the status.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "My purchases"))
    public ApiResult<List<PlanEnrollmentVo>> myEnrollments(@CurrentUser User student) {
        return planFacade.myEnrollments(student);
    }

    @GetMapping("/enrollments/{id}/instructions")
    @Operation(
            summary = "Where to pay for this plan, and how much",
            description = "The amount comes from the purchase, which froze the price when it was "
                    + "created. It is never read from the request - a caller that could name its "
                    + "own price would be the most obvious hole in a payment flow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment instructions"),
            @ApiResponse(responseCode = "403", description = "Not your purchase",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentInstructionsVo> instructions(@PathVariable Long id, @CurrentUser User caller) {
        return planFacade.paymentInstructions(id, caller);
    }

    @PostMapping(value = "/enrollments/{id}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Send proof of payment for a plan",
            description = "Multipart: the UTR plus a screenshot from the UPI app. Use the same "
                    + "call to resubmit after a rejection.\n\n"
                    + "JPG, PNG or WebP up to 5 MB. The type is detected from the file's own bytes "
                    + "rather than the Content-Type header, and the stored filename is generated "
                    + "server-side.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proof received, now SUBMITTED"),
            @ApiResponse(responseCode = "400", description = "Missing UTR, or the file was rejected",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not your purchase",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "Already paid, or already under review",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "413", description = "Screenshot too large",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanEnrollmentVo> submitProof(
            @PathVariable Long id,
            @Parameter(description = "UPI transaction / UTR number") @RequestParam String upiReference,
            @Parameter(description = "Screenshot from the UPI app") @RequestParam MultipartFile screenshot,
            @CurrentUser User student) {
        return planFacade.submitProof(id, upiReference, screenshot, student);
    }

    @PatchMapping("/enrollments/{id}/cancel")
    @Operation(summary = "Back out before paying")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled"),
            @ApiResponse(responseCode = "409", description = "Already active - an admin has to handle a refund",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PlanEnrollmentVo> cancel(@PathVariable Long id, @CurrentUser User student) {
        return planFacade.cancel(id, student);
    }

    @GetMapping("/enrollments/{id}/screenshot")
    @Operation(
            summary = "Download the payment screenshot",
            description = "Only the student who uploaded it and admins can fetch this - it is a "
                    + "picture of somebody's banking app.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The image"),
            @ApiResponse(responseCode = "403", description = "Not yours to view",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No screenshot uploaded",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ResponseEntity<Resource> screenshot(@PathVariable Long id, @CurrentUser User caller) {
        Path path = planFacade.screenshotPath(id, caller);
        String type = planFacade.screenshotContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type == null ? "application/octet-stream" : type))
                // attachment, not inline: never let an uploaded file render as a
                // page in our own origin.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}

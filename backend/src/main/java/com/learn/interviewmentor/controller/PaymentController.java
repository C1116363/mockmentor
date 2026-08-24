package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.payment.PaymentVo;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.facade.PaymentFacade;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "4. Payments",
        description = "Manual UPI payment. The student pays our UPI ID from their own app and "
                + "uploads a screenshot; an admin confirms the money arrived. No gateway is "
                + "involved in this version.")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    public PaymentController(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    @GetMapping("/instructions")
    @Operation(
            summary = "Where to pay and how much",
            description = "Everything the 'pay to book' popup shows: the UPI ID, the fee, and a "
                    + "`upi://` deep link that opens GPay / PhonePe / Paytm with the amount "
                    + "pre-filled on a phone.\n\n"
                    + "The amount comes from server config. It is never accepted from the client "
                    + "- a caller that could name its own price would be the most obvious hole "
                    + "in a payment flow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment instructions"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentInstructionsVo> instructions() {
        return paymentFacade.instructions();
    }

    @GetMapping("/by-request/{requestId}")
    @Operation(
            summary = "The payment for one booking",
            description = "Visible to the student who booked it and to admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The payment"),
            @ApiResponse(responseCode = "403", description = "Not your booking",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No payment for that request",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentVo> byRequest(
            @Parameter(description = "Interview request id", example = "12") @PathVariable Long requestId,
            @CurrentUser User caller) {
        return paymentFacade.forRequest(requestId, caller);
    }

    @PostMapping(value = "/by-request/{requestId}/proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Send proof of payment",
            description = "Multipart upload: the UTR / transaction reference plus a screenshot "
                    + "from the UPI app. Use the same call to resubmit after a rejection.\n\n"
                    + "Only JPG, PNG and WebP are accepted, up to 5 MB. The type is detected from "
                    + "the file's own bytes rather than the Content-Type header, and the stored "
                    + "filename is generated server-side.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proof received, now SUBMITTED"),
            @ApiResponse(responseCode = "400",
                    description = "Already paid, already under review, or the file was rejected",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not your booking",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentVo> submitProof(
            @Parameter(description = "Interview request id", example = "12") @PathVariable Long requestId,
            @Parameter(description = "UPI transaction / UTR number") @RequestParam String upiReference,
            @Parameter(description = "Screenshot from the UPI app") @RequestParam MultipartFile screenshot,
            @CurrentUser User student) {
        return paymentFacade.submitProof(requestId, upiReference, screenshot, student);
    }

    @GetMapping("/{id}/screenshot")
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
        Path path = paymentFacade.screenshotPath(id, caller);
        String type = paymentFacade.screenshotContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type == null ? "application/octet-stream" : type))
                // attachment, not inline: never let an uploaded file render as a
                // page in our own origin.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}

package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.dto.AssignMentorRequestDto;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import com.learn.interviewmentor.dto.mentor.RejectProfileRequestDto;
import com.learn.interviewmentor.vo.payment.PaymentVo;
import com.learn.interviewmentor.dto.payment.RejectPaymentRequestDto;
import com.learn.interviewmentor.facade.AdminFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Everything here is already locked to ADMIN by the URL rule in SecurityConfig.
 * The class-level @PreAuthorize is belt and braces.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "4. Admin", description = "**ADMIN only.** Every endpoint here returns 403 for anyone else.")
public class AdminController {

    private final AdminFacade adminFacade;

    public AdminController(AdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    // ---------------- mentor verification ----------------

    @GetMapping("/mentor-profiles/pending")
    @Operation(
            summary = "Mentors waiting to be verified",
            description = "The review queue, oldest submission first. Aadhaar and bank account "
                    + "numbers are masked to the last 4 digits - enough to check against a "
                    + "document, without exposing the full number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profiles awaiting review"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<MentorProfileVo>> pendingProfiles() {
        return adminFacade.pendingProfiles();
    }

    @GetMapping("/mentor-profiles")
    @Operation(summary = "Every mentor profile", description = "All statuses, newest submission first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All profiles"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<MentorProfileVo>> allProfiles() {
        return adminFacade.allProfiles();
    }

    @PatchMapping("/mentor-profiles/{id}/approve")
    @Operation(
            summary = "Verify a mentor",
            description = "Moves them to APPROVED. Only then can they see the interview queue "
                    + "and accept requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approved"),
            @ApiResponse(responseCode = "400", description = "The profile is not PENDING",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No profile with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorProfileVo> approveProfile(
            @Parameter(description = "Mentor profile id", example = "1") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminFacade.approveProfile(id, admin);
    }

    @PatchMapping("/mentor-profiles/{id}/reject")
    @Operation(
            summary = "Reject a mentor profile",
            description = "The reason is shown to the mentor so they can fix it and resubmit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "Not PENDING, or no reason given",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No profile with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorProfileVo> rejectProfile(
            @Parameter(description = "Mentor profile id", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectProfileRequestDto dto,
            @CurrentUser User admin) {
        return adminFacade.rejectProfile(id, dto.reason(), admin);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Platform statistics",
            description = "Counts by role and by request status - the numbers on the admin dashboard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Counts",
                    content = @Content(schema = @Schema(example =
                            "{\"students\":3,\"mentors\":5,\"admins\":1,\"totalRequests\":4,"
                          + "\"pending\":2,\"scheduled\":1,\"completed\":1,\"cancelled\":0}"))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<Map<String, Long>> stats() {
        return adminFacade.stats();
    }

    @GetMapping("/users")
    @Operation(
            summary = "List every user",
            description = "All three roles. Passwords are never included - `UserVo` has no such field.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Every account"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<UserVo>> allUsers() {
        return adminFacade.allUsers();
    }

    // ---------------- payments ----------------

    @GetMapping("/payments/pending")
    @Operation(
            summary = "Payments waiting to be checked",
            description = "Students who have sent a UPI reference and screenshot. Check the "
                    + "screenshot and the UTR against your bank, then verify or reject.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payments awaiting review"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<PaymentVo>> pendingPayments() {
        return adminFacade.pendingPayments();
    }

    @PatchMapping("/payments/{id}/verify")
    @Operation(
            summary = "Confirm the money arrived",
            description = "This is what releases the booking: the request moves from "
                    + "AWAITING_PAYMENT to PENDING and becomes visible to mentors. Until you "
                    + "verify, no mentor can see it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verified, booking released"),
            @ApiResponse(responseCode = "400", description = "The payment is not SUBMITTED",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No payment with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentVo> verifyPayment(
            @Parameter(description = "Payment id", example = "1") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminFacade.verifyPayment(id, admin);
    }

    @PatchMapping("/payments/{id}/reject")
    @Operation(
            summary = "Reject a payment",
            description = "The reason is shown to the student so they can send correct proof.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "Not SUBMITTED, or no reason given",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No payment with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<PaymentVo> rejectPayment(
            @Parameter(description = "Payment id", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectPaymentRequestDto dto,
            @CurrentUser User admin) {
        return adminFacade.rejectPayment(id, dto.reason(), admin);
    }

    // ---------------- interview requests ----------------

    @GetMapping("/requests/pending")
    @Operation(
            summary = "Requests waiting for a mentor",
            description = "Interview requests students have sent that nobody has picked up yet, "
                    + "oldest first. This is the admin's assignment queue.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unassigned requests"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<InterviewRequestVo>> pendingRequests() {
        return adminFacade.unassignedRequests();
    }

    @PatchMapping("/requests/{id}/assign")
    @Operation(
            summary = "Attach a mentor to a student's request",
            description = "Hands a pending request to a specific mentor and schedules it, moving "
                    + "it from PENDING to SCHEDULED.\n\n"
                    + "This is the second route to scheduling: a mentor can claim a request from "
                    + "the queue themselves, or an admin can assign one directly.\n\n"
                    + "Leave `scheduledAt` out and the student's own requested slot is used.\n\n"
                    + "The mentor must be **verified** - assigning an unapproved mentor would "
                    + "sidestep the whole verification step, so it is refused.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned and scheduled"),
            @ApiResponse(responseCode = "400",
                    description = "Request is not PENDING, or that user is not a verified, active mentor",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No request or no such user",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<InterviewRequestVo> assignMentor(
            @Parameter(description = "Interview request id", example = "1") @PathVariable Long id,
            @Valid @RequestBody AssignMentorRequestDto dto,
            @CurrentUser User admin) {
        return adminFacade.assignMentor(id, dto, admin);
    }

    @GetMapping("/requests")
    @Operation(
            summary = "List every interview request",
            description = "Across all students and mentors, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Every request"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<InterviewRequestVo>> allRequests() {
        return adminFacade.allRequests();
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(
            summary = "Block an account",
            description = "Sets `active = false`. That user can no longer log in - the check "
                    + "happens in `AppUserDetails.isEnabled()`. You cannot deactivate yourself.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account blocked"),
            @ApiResponse(responseCode = "400", description = "You tried to deactivate your own admin account",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No user with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<UserVo> deactivate(
            @Parameter(description = "Id of the user to block", example = "5") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminFacade.setUserActive(id, false, admin);
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(
            summary = "Unblock an account",
            description = "Sets `active = true` so the user can log in again.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account unblocked"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No user with that id",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<UserVo> activate(
            @Parameter(description = "Id of the user to unblock", example = "5") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminFacade.setUserActive(id, true, admin);
    }
}

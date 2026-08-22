package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.dto.AssignMentorRequest;
import com.learn.interviewmentor.dto.InterviewRequestDto;
import com.learn.interviewmentor.dto.auth.UserDto;
import com.learn.interviewmentor.dto.mentor.MentorProfileDto;
import com.learn.interviewmentor.dto.mentor.RejectProfileRequest;
import com.learn.interviewmentor.dto.payment.PaymentDto;
import com.learn.interviewmentor.dto.payment.RejectPaymentRequest;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.service.AdminService;
import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.service.MentorProfileService;
import com.learn.interviewmentor.service.PaymentService;
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

    private final AdminService adminService;
    private final InterviewRequestService requestService;
    private final MentorProfileService profileService;
    private final PaymentService paymentService;

    public AdminController(AdminService adminService,
                           InterviewRequestService requestService,
                           MentorProfileService profileService,
                           PaymentService paymentService) {
        this.adminService = adminService;
        this.requestService = requestService;
        this.profileService = profileService;
        this.paymentService = paymentService;
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
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<MentorProfileDto> pendingProfiles() {
        return profileService.awaitingReview();
    }

    @GetMapping("/mentor-profiles")
    @Operation(summary = "Every mentor profile", description = "All statuses, newest submission first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All profiles"),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<MentorProfileDto> allProfiles() {
        return profileService.allProfiles();
    }

    @PatchMapping("/mentor-profiles/{id}/approve")
    @Operation(
            summary = "Verify a mentor",
            description = "Moves them to APPROVED. Only then can they see the interview queue "
                    + "and accept requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approved"),
            @ApiResponse(responseCode = "400", description = "The profile is not PENDING",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No profile with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public MentorProfileDto approveProfile(
            @Parameter(description = "Mentor profile id", example = "1") @PathVariable Long id,
            @CurrentUser User admin) {
        return profileService.approve(id, admin);
    }

    @PatchMapping("/mentor-profiles/{id}/reject")
    @Operation(
            summary = "Reject a mentor profile",
            description = "The reason is shown to the mentor so they can fix it and resubmit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "Not PENDING, or no reason given",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No profile with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public MentorProfileDto rejectProfile(
            @Parameter(description = "Mentor profile id", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectProfileRequest dto,
            @CurrentUser User admin) {
        return profileService.reject(id, dto.reason(), admin);
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
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public Map<String, Long> stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    @Operation(
            summary = "List every user",
            description = "All three roles. Passwords are never included - `UserDto` has no such field.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Every account"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<UserDto> allUsers() {
        return adminService.findAllUsers();
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
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<PaymentDto> pendingPayments() {
        return paymentService.awaitingReview();
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
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No payment with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public PaymentDto verifyPayment(
            @Parameter(description = "Payment id", example = "1") @PathVariable Long id,
            @CurrentUser User admin) {
        return paymentService.verify(id, admin);
    }

    @PatchMapping("/payments/{id}/reject")
    @Operation(
            summary = "Reject a payment",
            description = "The reason is shown to the student so they can send correct proof.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "Not SUBMITTED, or no reason given",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No payment with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public PaymentDto rejectPayment(
            @Parameter(description = "Payment id", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectPaymentRequest dto,
            @CurrentUser User admin) {
        return paymentService.reject(id, dto.reason(), admin);
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
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<InterviewRequestDto> pendingRequests() {
        return requestService.findPendingForAdmin();
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
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No request or no such user",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public InterviewRequestDto assignMentor(
            @Parameter(description = "Interview request id", example = "1") @PathVariable Long id,
            @Valid @RequestBody AssignMentorRequest dto,
            @CurrentUser User admin) {
        return requestService.assignMentor(id, dto, admin);
    }

    @GetMapping("/requests")
    @Operation(
            summary = "List every interview request",
            description = "Across all students and mentors, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Every request"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<InterviewRequestDto> allRequests() {
        return requestService.findAll();
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(
            summary = "Block an account",
            description = "Sets `active = false`. That user can no longer log in - the check "
                    + "happens in `AppUserDetails.isEnabled()`. You cannot deactivate yourself.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account blocked"),
            @ApiResponse(responseCode = "400", description = "You tried to deactivate your own admin account",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No user with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public UserDto deactivate(
            @Parameter(description = "Id of the user to block", example = "5") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminService.setActive(id, false, admin);
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(
            summary = "Unblock an account",
            description = "Sets `active = true` so the user can log in again.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account unblocked"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No user with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public UserDto activate(
            @Parameter(description = "Id of the user to unblock", example = "5") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminService.setActive(id, true, admin);
    }
}

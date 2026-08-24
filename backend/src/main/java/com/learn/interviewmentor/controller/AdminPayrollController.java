package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.payroll.MarkPaidDto;
import com.learn.interviewmentor.dto.payroll.PayrollSettingsDto;
import com.learn.interviewmentor.facade.PayrollFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.vo.payroll.MentorPayoutVo;
import com.learn.interviewmentor.vo.payroll.MentorPayrollVo;
import com.learn.interviewmentor.vo.payroll.PayrollSummaryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payroll")
@Validated
@Tag(name = "9. Payroll (admin)",
        description = """
                Paying mentors for the sessions they have run.

                A completed session is paid **exactly once**, and the thing that guarantees
                it is a column - `interview_requests.payout_id` - not a date range and not a
                careful process. Raising a payout stamps every unpaid session with its id in
                a single UPDATE, so two admins pressing the button at the same moment cannot
                both claim the same work.

                Amounts are never accepted from the client. The rates come from the mentor's
                profile and the counts come from what was actually claimed.
                """)
public class AdminPayrollController {

    private final PayrollFacade payrollFacade;

    public AdminPayrollController(PayrollFacade payrollFacade) {
        this.payrollFacade = payrollFacade;
    }

    @GetMapping("/mentors")
    @Operation(
            summary = "Every mentor, their rates, and what they are owed",
            description = "The payroll screen. Includes bank details so an admin can make the "
                    + "transfer without leaving the page, and `bankDetailsComplete` so it can "
                    + "warn before raising a payout for somebody who cannot actually be paid.")
    public ApiResult<List<MentorPayrollVo>> mentors() {
        return payrollFacade.mentors();
    }

    @GetMapping("/summary")
    @Operation(summary = "Totals across all mentors",
            description = "How many are on payroll, what is collectively owed, what is raised "
                    + "but unpaid, and what has been paid to date.")
    public ApiResult<PayrollSummaryVo> summary() {
        return payrollFacade.summary();
    }

    @PatchMapping("/mentors/{mentorId}/settings")
    @Operation(
            summary = "Turn payroll on for a mentor, and set what they earn",
            description = """
                    Two rates, because a mock interview ends in a written scorecard and a
                    mentoring session does not - different work, usually different money.

                    Turning payroll **off** keeps the rates, so somebody stepping away for a
                    month comes back to their old numbers instead of a blank form. Enabling
                    without both rates is refused rather than left to fail later at the
                    payout.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved"),
            @ApiResponse(responseCode = "400", description = "Enabled without both rates set",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No mentor profile for that user",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorPayrollVo> configure(
            @Parameter(description = "The mentor's user id", example = "3") @PathVariable Long mentorId,
            @Valid @RequestBody PayrollSettingsDto dto) {
        return payrollFacade.configure(mentorId, dto);
    }

    @PostMapping("/mentors/{mentorId}/payouts")
    @Operation(
            summary = "Raise a payout for everything this mentor is owed",
            description = """
                    Claims every completed session that is not already in a payout, freezes
                    the rates, and totals it up.

                    **There is no amount in the request body**, and there must never be one -
                    the figure is computed from the sessions actually claimed. A caller that
                    could name the amount could name anyone's wages.

                    Refused when the mentor already has a payout awaiting payment: the second
                    would claim nothing, because the first took every session.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payout raised - now send the money"),
            @ApiResponse(responseCode = "400",
                    description = "Payroll is off, rates are missing, or nothing is owed",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "A payout is already awaiting payment",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorPayoutVo> createPayout(
            @Parameter(description = "The mentor's user id", example = "3") @PathVariable Long mentorId,
            @CurrentUser User admin) {
        return payrollFacade.createPayout(mentorId, admin);
    }

    @PatchMapping("/payouts/{payoutId}/mark-paid")
    @Operation(
            summary = "Record that the money has been sent",
            description = "The bank reference is required, not optional. Its whole value shows "
                    + "up months later, when a mentor says they were not paid and the reference "
                    + "is the thing that settles it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recorded as paid"),
            @ApiResponse(responseCode = "409", description = "Already paid, or cancelled",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorPayoutVo> markPaid(
            @PathVariable Long payoutId,
            @Valid @RequestBody MarkPaidDto dto,
            @CurrentUser User admin) {
        return payrollFacade.markPaid(payoutId, dto, admin);
    }

    @PatchMapping("/payouts/{payoutId}/cancel")
    @Operation(
            summary = "Undo a payout raised in error",
            description = """
                    Releases its sessions so they are owed again and get picked up by the next
                    payout.

                    **A paid payout cannot be cancelled.** That would put work that has already
                    been paid for back in line to be paid a second time - the exact failure
                    this design exists to prevent, reintroduced through the undo button. A
                    payment sent in error needs a correcting entry, not a rewrite of history.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled, sessions released"),
            @ApiResponse(responseCode = "409", description = "Already paid, or already cancelled",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<MentorPayoutVo> cancelPayout(
            @PathVariable Long payoutId,
            @Parameter(description = "Why - shown in the payout history")
            @RequestParam @NotBlank(message = "Say why this payout is being cancelled")
            @Size(max = 500) String reason,
            @CurrentUser User admin) {
        return payrollFacade.cancelPayout(payoutId, reason, admin);
    }

    @GetMapping("/payouts")
    @Operation(summary = "Every payout, newest first")
    public ApiResult<List<MentorPayoutVo>> payouts() {
        return payrollFacade.payouts();
    }

    @GetMapping("/mentors/{mentorId}/payouts")
    @Operation(summary = "One mentor's payout history")
    public ApiResult<List<MentorPayoutVo>> payoutsFor(
            @Parameter(description = "The mentor's user id", example = "3") @PathVariable Long mentorId) {
        return payrollFacade.payoutsFor(mentorId);
    }
}

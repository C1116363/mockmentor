package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.payroll.MarkPaidDto;
import com.learn.interviewmentor.dto.payroll.PayrollSettingsDto;
import com.learn.interviewmentor.facade.PayrollFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.PayrollService;
import com.learn.interviewmentor.vo.payroll.MentorPayoutVo;
import com.learn.interviewmentor.vo.payroll.MentorPayrollVo;
import com.learn.interviewmentor.vo.payroll.PayrollSummaryVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayrollFacadeImpl implements PayrollFacade {

    private final PayrollService payrollService;

    public PayrollFacadeImpl(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @Override
    public ApiResult<List<MentorPayrollVo>> mentors() {
        return ApiResult.ok(payrollService.allMentors());
    }

    @Override
    public ApiResult<PayrollSummaryVo> summary() {
        return ApiResult.ok(payrollService.summary());
    }

    @Override
    public ApiResult<MentorPayrollVo> configure(Long mentorId, PayrollSettingsDto request) {
        MentorPayrollVo updated = payrollService.configure(mentorId, request);
        return ApiResult.ok(updated, updated.payrollEnabled()
                ? "Payroll is on for " + updated.mentorName() + "."
                : "Payroll is off for " + updated.mentorName() + ". Their rates are kept.");
    }

    /**
     * The message names the figures rather than just saying "done".
     *
     * This is the step that decides what somebody gets paid, so the numbers
     * belong in front of the admin at the moment it happens - not one screen
     * away behind a click they might not make.
     */
    @Override
    public ApiResult<MentorPayoutVo> createPayout(Long mentorId, User admin) {
        MentorPayoutVo payout = payrollService.createPayout(mentorId, admin);
        return ApiResult.created(payout, String.format(
                "Payout raised for %s: %d interview%s + %d mentoring session%s = ₹%s. "
                        + "Send the money, then mark it paid with the bank reference.",
                payout.mentorName(),
                payout.interviewCount(), payout.interviewCount() == 1 ? "" : "s",
                payout.mentoringCount(), payout.mentoringCount() == 1 ? "" : "s",
                payout.amount().toPlainString()));
    }

    @Override
    public ApiResult<MentorPayoutVo> markPaid(Long payoutId, MarkPaidDto request, User admin) {
        MentorPayoutVo payout = payrollService.markPaid(payoutId, request, admin);
        return ApiResult.ok(payout, String.format("₹%s to %s recorded as paid.",
                payout.amount().toPlainString(), payout.mentorName()));
    }

    @Override
    public ApiResult<MentorPayoutVo> cancelPayout(Long payoutId, String reason, User admin) {
        MentorPayoutVo payout = payrollService.cancelPayout(payoutId, reason, admin);
        return ApiResult.ok(payout, String.format(
                "Payout cancelled. Its %d %s owed again and will be picked up by the "
                        + "next payout for %s.",
                payout.totalSessions(),
                payout.totalSessions() == 1 ? "session is" : "sessions are",
                payout.mentorName()));
    }

    @Override
    public ApiResult<List<MentorPayoutVo>> payouts() {
        return ApiResult.ok(payrollService.allPayouts());
    }

    @Override
    public ApiResult<List<MentorPayoutVo>> payoutsFor(Long mentorId) {
        return ApiResult.ok(payrollService.payoutsFor(mentorId));
    }
}

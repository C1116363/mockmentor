package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.payroll.MarkPaidDto;
import com.learn.interviewmentor.dto.payroll.PayrollSettingsDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.payroll.MentorPayoutVo;
import com.learn.interviewmentor.vo.payroll.MentorPayrollVo;
import com.learn.interviewmentor.vo.payroll.PayrollSummaryVo;

import java.util.List;

/**
 * Paying mentors for the sessions they have run.
 *
 * <h2>The one rule everything else follows from</h2>
 * A completed session is paid <b>exactly once</b>. Not "usually once" - the
 * failure here is somebody's wages, and both directions are bad: paying twice
 * costs money and is awkward to claw back, paying zero times means a mentor
 * worked for free and had to notice before anyone else did.
 *
 * What guarantees it is not a date range or a careful process. It is a column:
 * {@code InterviewRequest.payout}. A session with a stamp is spent; a session
 * without one is owed. The stamp is applied by a single UPDATE whose WHERE
 * clause includes {@code payout_id is null}, so concurrent runs cannot both
 * claim it - the database picks the winner.
 *
 * <h2>The flow</h2>
 * <ol>
 *   <li>Admin turns payroll on for a mentor and sets two rates.</li>
 *   <li>Sessions accumulate as the mentor completes them.</li>
 *   <li>Admin raises a payout - this claims every unpaid session and freezes
 *       the rates and the total.</li>
 *   <li>Admin sends the money and records the bank reference.</li>
 * </ol>
 * A payout raised in error can be cancelled, which puts its sessions back in
 * the pot. A <i>paid</i> one cannot - see the implementation.
 */
public interface PayrollService {

    /** Every mentor, their rates, and what they are owed. The main screen. */
    List<MentorPayrollVo> allMentors();

    /** Totals across everyone, for the section header. */
    PayrollSummaryVo summary();

    /** Turn payroll on or off for one mentor, and set what they earn. */
    MentorPayrollVo configure(Long mentorId, PayrollSettingsDto dto);

    /**
     * Raise a payout covering everything this mentor is currently owed.
     *
     * The amount is computed from what was actually claimed, never from a
     * figure the caller supplies - there is no amount field on the way in, and
     * there must not be one.
     */
    MentorPayoutVo createPayout(Long mentorId, User admin);

    /** Record that the money has gone. */
    MentorPayoutVo markPaid(Long payoutId, MarkPaidDto dto, User admin);

    /** Undo a payout raised in error, releasing its sessions to be paid later. */
    MentorPayoutVo cancelPayout(Long payoutId, String reason, User admin);

    List<MentorPayoutVo> allPayouts();

    List<MentorPayoutVo> payoutsFor(Long mentorId);

    /** How many payouts are raised and waiting to be paid. Drives the tab badge. */
    long countPending();
}

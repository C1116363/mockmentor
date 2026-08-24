package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.payroll.MarkPaidDto;
import com.learn.interviewmentor.dto.payroll.PayrollSettingsDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.payroll.MentorPayoutVo;
import com.learn.interviewmentor.vo.payroll.MentorPayrollVo;
import com.learn.interviewmentor.vo.payroll.PayrollSummaryVo;

import java.util.List;

/** Paying mentors for the sessions they have run. Admin only. */
public interface PayrollFacade {

    ApiResult<List<MentorPayrollVo>> mentors();

    ApiResult<PayrollSummaryVo> summary();

    ApiResult<MentorPayrollVo> configure(Long mentorId, PayrollSettingsDto request);

    ApiResult<MentorPayoutVo> createPayout(Long mentorId, User admin);

    ApiResult<MentorPayoutVo> markPaid(Long payoutId, MarkPaidDto request, User admin);

    ApiResult<MentorPayoutVo> cancelPayout(Long payoutId, String reason, User admin);

    ApiResult<List<MentorPayoutVo>> payouts();

    ApiResult<List<MentorPayoutVo>> payoutsFor(Long mentorId);
}

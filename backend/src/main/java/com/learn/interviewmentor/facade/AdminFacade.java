package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.AssignMentorRequestDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import com.learn.interviewmentor.vo.payment.PaymentVo;

import java.util.List;
import java.util.Map;

/**
 * Everything only an admin does, for interviews, mentors, users and payments.
 *
 * Plans and study material have their own facades rather than being folded in
 * here - an admin-shaped bag that grows a method per feature is the thing this
 * layer is supposed to prevent. What lives here is what has no better home:
 * the dashboard tiles, the mentor verification queue, and account control.
 */
public interface AdminFacade {

    ApiResult<Map<String, Long>> stats();

    ApiResult<List<UserVo>> allUsers();

    ApiResult<UserVo> setUserActive(Long userId, boolean active, User admin);

    // ---- mentor verification ----
    ApiResult<List<MentorProfileVo>> pendingProfiles();

    ApiResult<List<MentorProfileVo>> allProfiles();

    ApiResult<MentorProfileVo> approveProfile(Long id, User admin);

    ApiResult<MentorProfileVo> rejectProfile(Long id, String reason, User admin);

    // ---- sessions ----
    ApiResult<List<InterviewRequestVo>> unassignedRequests();

    ApiResult<List<InterviewRequestVo>> allRequests();

    ApiResult<InterviewRequestVo> assignMentor(Long requestId, AssignMentorRequestDto request,
                                               User admin);

    // ---- interview payments ----
    ApiResult<List<PaymentVo>> pendingPayments();

    ApiResult<PaymentVo> verifyPayment(Long id, User admin);

    ApiResult<PaymentVo> rejectPayment(Long id, String reason, User admin);
}

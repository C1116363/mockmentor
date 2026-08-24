package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.AssignMentorRequestDto;
import com.learn.interviewmentor.facade.AdminFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.AdminService;
import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.service.MentorProfileService;
import com.learn.interviewmentor.service.PaymentService;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import com.learn.interviewmentor.vo.payment.PaymentVo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AdminFacadeImpl implements AdminFacade {

    private final AdminService adminService;
    private final MentorProfileService profileService;
    private final InterviewRequestService requestService;
    private final PaymentService paymentService;

    public AdminFacadeImpl(AdminService adminService,
                           MentorProfileService profileService,
                           InterviewRequestService requestService,
                           PaymentService paymentService) {
        this.adminService = adminService;
        this.profileService = profileService;
        this.requestService = requestService;
        this.paymentService = paymentService;
    }

    @Override
    public ApiResult<Map<String, Long>> stats() {
        return ApiResult.ok(adminService.stats());
    }

    @Override
    public ApiResult<List<UserVo>> allUsers() {
        return ApiResult.ok(adminService.findAllUsers());
    }

    @Override
    public ApiResult<UserVo> setUserActive(Long userId, boolean active, User admin) {
        UserVo user = adminService.setActive(userId, active, admin);
        return ApiResult.ok(user, active
                ? user.fullName() + " can log in again."
                : user.fullName() + " is blocked from logging in.");
    }

    // ---- mentor verification ----

    @Override
    public ApiResult<List<MentorProfileVo>> pendingProfiles() {
        return ApiResult.ok(profileService.awaitingReview());
    }

    @Override
    public ApiResult<List<MentorProfileVo>> allProfiles() {
        return ApiResult.ok(profileService.allProfiles());
    }

    @Override
    public ApiResult<MentorProfileVo> approveProfile(Long id, User admin) {
        MentorProfileVo profile = profileService.approve(id, admin);
        return ApiResult.ok(profile, profile.fullName() + " is now verified.");
    }

    @Override
    public ApiResult<MentorProfileVo> rejectProfile(Long id, String reason, User admin) {
        MentorProfileVo profile = profileService.reject(id, reason, admin);
        return ApiResult.ok(profile, profile.fullName() + " was rejected.");
    }

    // ---- sessions ----

    @Override
    public ApiResult<List<InterviewRequestVo>> unassignedRequests() {
        return ApiResult.ok(requestService.findPendingForAdmin());
    }

    @Override
    public ApiResult<List<InterviewRequestVo>> allRequests() {
        return ApiResult.ok(requestService.findAll());
    }

    @Override
    public ApiResult<InterviewRequestVo> assignMentor(Long requestId,
                                                      AssignMentorRequestDto request, User admin) {
        InterviewRequestVo assigned = requestService.assignMentor(requestId, request, admin);
        return ApiResult.ok(assigned,
                "Assigned to " + assigned.mentor().name() + ". The student can see it now.");
    }

    // ---- interview payments ----

    @Override
    public ApiResult<List<PaymentVo>> pendingPayments() {
        return ApiResult.ok(paymentService.awaitingReview());
    }

    @Override
    public ApiResult<PaymentVo> verifyPayment(Long id, User admin) {
        PaymentVo payment = paymentService.verify(id, admin);
        return ApiResult.ok(payment,
                "Payment confirmed. " + payment.studentName()
                        + "'s session is now open to mentors.");
    }

    @Override
    public ApiResult<PaymentVo> rejectPayment(Long id, String reason, User admin) {
        return ApiResult.ok(paymentService.reject(id, reason, admin),
                "Payment rejected. The student can send new proof.");
    }
}

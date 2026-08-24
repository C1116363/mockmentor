package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.plan.PlanRequestDto;
import com.learn.interviewmentor.facade.PlanFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.PlanEnrollmentService;
import com.learn.interviewmentor.service.PlanService;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.plan.PlanEnrollmentVo;
import com.learn.interviewmentor.vo.plan.PlanVo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Component
public class PlanFacadeImpl implements PlanFacade {

    private final PlanService planService;
    private final PlanEnrollmentService enrollmentService;

    public PlanFacadeImpl(PlanService planService, PlanEnrollmentService enrollmentService) {
        this.planService = planService;
        this.enrollmentService = enrollmentService;
    }

    // ---------- browse ----------

    @Override
    public ApiResult<List<PlanVo>> activePlans() {
        return ApiResult.ok(planService.activePlans());
    }

    @Override
    public ApiResult<PlanVo> plan(Long id) {
        return ApiResult.ok(planService.one(id));
    }

    // ---------- buying ----------

    @Override
    public ApiResult<PlanEnrollmentVo> enroll(Long planId, User student) {
        PlanEnrollmentVo enrollment = enrollmentService.enroll(planId, student);
        return ApiResult.created(enrollment);
    }

    @Override
    public ApiResult<List<PlanEnrollmentVo>> myEnrollments(User student) {
        return ApiResult.ok(enrollmentService.mine(student));
    }

    @Override
    public ApiResult<PaymentInstructionsVo> paymentInstructions(Long enrollmentId, User caller) {
        return ApiResult.ok(enrollmentService.instructionsFor(enrollmentId, caller));
    }

    @Override
    public ApiResult<PlanEnrollmentVo> submitProof(Long enrollmentId, String upiReference,
                                                   MultipartFile screenshot, User student) {
        PlanEnrollmentVo saved =
                enrollmentService.submitProof(enrollmentId, upiReference, screenshot, student);
        return ApiResult.ok(saved,
                "Thanks - we're checking your payment. Your plan unlocks once an admin confirms it.");
    }

    @Override
    public ApiResult<PlanEnrollmentVo> cancel(Long enrollmentId, User student) {
        return ApiResult.ok(enrollmentService.cancel(enrollmentId, student), "Purchase cancelled.");
    }

    @Override
    public Path screenshotPath(Long enrollmentId, User caller) {
        return enrollmentService.screenshotPath(enrollmentId, caller);
    }

    @Override
    public String screenshotContentType(Long enrollmentId) {
        return enrollmentService.screenshotContentType(enrollmentId);
    }

    // ---------- admin ----------

    @Override
    public ApiResult<List<PlanVo>> allPlans() {
        return ApiResult.ok(planService.allPlans());
    }

    @Override
    public ApiResult<PlanVo> create(PlanRequestDto request) {
        PlanVo plan = planService.create(request);
        return ApiResult.created(plan);
    }

    @Override
    public ApiResult<PlanVo> update(Long id, PlanRequestDto request) {
        return ApiResult.ok(planService.update(id, request), "Plan saved.");
    }

    @Override
    public ApiResult<PlanVo> changePrice(Long id, PlanPriceRequestDto request) {
        PlanVo plan = planService.changePrice(id, request);
        // The consequence is the reason an admin came here, so it is worth saying.
        return ApiResult.ok(plan, plan.name() + " is now ₹" + plan.price()
                + ". Students see it on their next page load; anyone who already bought it "
                + "keeps the price they paid.");
    }

    @Override
    public ApiResult<PlanVo> setActive(Long id, boolean active) {
        PlanVo plan = planService.setActive(id, active);
        return ApiResult.ok(plan, active
                ? plan.name() + " is back on sale."
                : plan.name() + " is off sale. Students who already bought it keep their access.");
    }

    @Override
    public ApiResult<List<PlanEnrollmentVo>> pendingEnrollments() {
        return ApiResult.ok(enrollmentService.awaitingReview());
    }

    @Override
    public ApiResult<List<PlanEnrollmentVo>> allEnrollments() {
        return ApiResult.ok(enrollmentService.all());
    }

    @Override
    public ApiResult<PlanEnrollmentVo> activateEnrollment(Long id, User admin) {
        PlanEnrollmentVo e = enrollmentService.activate(id, admin);
        return ApiResult.ok(e, e.studentName() + " now has " + e.planName() + ".");
    }

    @Override
    public ApiResult<PlanEnrollmentVo> rejectEnrollment(Long id, String reason, User admin) {
        return ApiResult.ok(enrollmentService.reject(id, reason, admin),
                "Rejected. The student can send new proof.");
    }
}

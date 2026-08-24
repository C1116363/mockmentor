package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.plan.PlanRequestDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.plan.PlanEnrollmentVo;
import com.learn.interviewmentor.vo.plan.PlanVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Plans and plan purchases, as one use case per method.
 *
 * <h2>What a facade is for here</h2>
 * The controller's job shrinks to HTTP: read the request, hand it over, return
 * what comes back. The facade owns the use case - which services to call, in
 * what order, and what the caller should be told. The service below it owns the
 * rules for one thing.
 *
 * You can see the split earning its keep in {@link #enroll}: buying a plan
 * touches the price list and the enrollment ledger, and deciding that those two
 * calls belong together is a use-case decision, not something either service
 * should know about the other.
 *
 * <h2>Where the envelope is applied</h2>
 * Here, not in the controller. The message that goes with a result is part of
 * the use case - "students see it on their next load" is the point of changing a
 * price - and a controller assembling that sentence would be business language
 * in the HTTP layer.
 */
public interface PlanFacade {

    // ---------- browse ----------

    ApiResult<List<PlanVo>> activePlans();

    ApiResult<PlanVo> plan(Long id);

    // ---------- buying ----------

    ApiResult<PlanEnrollmentVo> enroll(Long planId, User student);

    ApiResult<List<PlanEnrollmentVo>> myEnrollments(User student);

    ApiResult<PaymentInstructionsVo> paymentInstructions(Long enrollmentId, User caller);

    ApiResult<PlanEnrollmentVo> submitProof(Long enrollmentId, String upiReference,
                                            MultipartFile screenshot, User student);

    ApiResult<PlanEnrollmentVo> cancel(Long enrollmentId, User student);

    /**
     * Returns a bare Path, not an envelope.
     *
     * A file download is the one thing the envelope cannot wrap - the body has to
     * be the bytes. Wrapping it would mean base64 inside JSON, which triples the
     * size and forces the browser to decode it before it can save it.
     */
    Path screenshotPath(Long enrollmentId, User caller);

    String screenshotContentType(Long enrollmentId);

    // ---------- admin ----------

    ApiResult<List<PlanVo>> allPlans();

    ApiResult<PlanVo> create(PlanRequestDto request);

    ApiResult<PlanVo> update(Long id, PlanRequestDto request);

    ApiResult<PlanVo> changePrice(Long id, PlanPriceRequestDto request);

    ApiResult<PlanVo> setActive(Long id, boolean active);

    ApiResult<List<PlanEnrollmentVo>> pendingEnrollments();

    ApiResult<List<PlanEnrollmentVo>> allEnrollments();

    ApiResult<PlanEnrollmentVo> activateEnrollment(Long id, User admin);

    ApiResult<PlanEnrollmentVo> rejectEnrollment(Long id, String reason, User admin);
}

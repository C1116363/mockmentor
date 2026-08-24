package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.plan.PlanEnrollmentVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.EnrollmentStatus;
import com.learn.interviewmentor.model.Plan;
import com.learn.interviewmentor.model.PlanEnrollment;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.PlanEnrollmentRepository;
import com.learn.interviewmentor.storage.ScreenshotStorage;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Buying a plan: the same manual UPI flow as an interview booking.
 *
 * Student picks a plan -> AWAITING_PAYMENT -> they pay our UPI ID from their own
 * app and upload a screenshot -> SUBMITTED -> an admin checks the UTR against
 * the bank -> ACTIVE, and the clock starts.
 *
 * The amount charged is read from the plan on the server, never from the request
 * body. A client that could name its own price would be the most obvious hole in
 * a payment flow, and it is the same rule PaymentService follows for interviews.
 *
 * Screenshot storage is shared with the interview flow - it is the same kind of
 * file, with the same rules, so it would be strange to have two of them.
 */
public interface PlanEnrollmentService {

    /**
    * Student chooses a plan.
    *
    * Idempotent on purpose: if they already have a live enrollment for this
    * plan we hand that one back instead of creating a second. A double-tap on
    * "Get this plan" is the normal way this endpoint gets called twice, and two
    * rows would mean two payment screens for one purchase.
    */
    PlanEnrollmentVo enroll(Long planId, User student);

    List<PlanEnrollmentVo> mine(User student);

    /** Where to pay and how much, for one enrollment. */
    PaymentInstructionsVo instructionsFor(Long enrollmentId, User caller);

    /** Student sends proof. Also the resubmit path after a rejection. */
    PlanEnrollmentVo submitProof(Long enrollmentId, String upiReference, MultipartFile screenshot, User student);

    PlanEnrollmentVo cancel(Long enrollmentId, User student);

    List<PlanEnrollmentVo> awaitingReview();

    List<PlanEnrollmentVo> all();

    /** Admin confirms the money arrived. This is what grants access. */
    PlanEnrollmentVo activate(Long enrollmentId, User admin);

    PlanEnrollmentVo reject(Long enrollmentId, String reason, User admin);

    long countAwaitingReview();

    long countActive();

    /** A picture of somebody's banking app: owner and admins only. */
    Path screenshotPath(Long enrollmentId, User caller);

    String screenshotContentType(Long enrollmentId);

    /** Plan ids this student currently holds. Used to gate study material. */
    List<Long> activePlanIds(User student);
}

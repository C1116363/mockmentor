package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.payment.PurchaseSettlement;
import com.learn.interviewmentor.service.PaymentService;
import com.learn.interviewmentor.service.PlanEnrollmentService;
import com.learn.interviewmentor.service.PlanService;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@Service
@Transactional(readOnly = true)
public class PlanEnrollmentServiceImpl implements PlanEnrollmentService, PurchaseSettlement {

    private static final Logger log = LoggerFactory.getLogger(PlanEnrollmentServiceImpl.class);

    private final PlanEnrollmentRepository enrollmentRepository;
    private final PlanService planService;
    private final ScreenshotStorage storage;

    private final String upiId;
    private final String payeeName;

    public PlanEnrollmentServiceImpl(PlanEnrollmentRepository enrollmentRepository,
                                 PlanService planService,
                                 ScreenshotStorage storage,
                                 @Value("${app.payment.upi-id}") String upiId,
                                 @Value("${app.payment.payee-name}") String payeeName) {
        this.enrollmentRepository = enrollmentRepository;
        this.planService = planService;
        this.storage = storage;
        this.upiId = upiId;
        this.payeeName = payeeName;
    }

    // ---------- student ----------

    /**
     * Student chooses a plan.
     *
     * Idempotent on purpose: if they already have a live enrollment for this
     * plan we hand that one back instead of creating a second. A double-tap on
     * "Get this plan" is the normal way this endpoint gets called twice, and two
     * rows would mean two payment screens for one purchase.
     */
    @Transactional
    @Override
    public PlanEnrollmentVo enroll(Long planId, User student) {
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can buy a plan");
        }

        Plan plan = planService.activeEntity(planId);

        var existing = enrollmentRepository.findLatestLive(student.getId(), planId);
        if (existing.isPresent()) {
            PlanEnrollment live = existing.get();
            if (live.isCurrentlyActive()) {
                throw new ConflictException("You already have " + plan.getName() + " running");
            }
            // Still mid-purchase - send them back to the same one.
            return PlanEnrollmentVo.from(live);
        }

        PlanEnrollment saved = enrollmentRepository.save(new PlanEnrollment(plan, student));
        log.info("{} started buying plan '{}' at {}",
                student.getEmail(), plan.getName(), saved.getPricePaid());
        return PlanEnrollmentVo.from(saved);
    }

    @Override
    public List<PlanEnrollmentVo> mine(User student) {
        return enrollmentRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(PlanEnrollmentVo::from).toList();
    }

    /** Where to pay and how much, for one enrollment. */
    @Override
    public PaymentInstructionsVo instructionsFor(Long enrollmentId, User caller) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);
        assertCanSee(enrollment, caller);

        BigDecimal amount = enrollment.getPricePaid();
        String link = "upi://pay"
                + "?pa=" + enc(upiId)
                + "&pn=" + enc(payeeName)
                + "&am=" + amount.toPlainString()
                + "&cu=INR";
        return new PaymentInstructionsVo(upiId, payeeName, amount, "INR", link);
    }

    /** Student sends proof. Also the resubmit path after a rejection. */
    @Transactional
    @Override
    public PlanEnrollmentVo submitProof(Long enrollmentId, String upiReference,
                                         MultipartFile screenshot, User student) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);

        if (!enrollment.isOwnedBy(student)) {
            throw new ForbiddenException("That isn't your purchase");
        }
        if (upiReference == null || upiReference.isBlank()) {
            throw new BadRequestException("Enter the UPI transaction / UTR number");
        }

        // 409, not 400: nothing is wrong with the upload, the purchase has just
        // already moved past this step.
        switch (enrollment.getStatus()) {
            case ACTIVE -> throw new ConflictException("You have already paid for this plan");
            case SUBMITTED -> throw new ConflictException("We're already checking your last screenshot");
            case CANCELLED -> throw new ConflictException("This purchase was cancelled. Start again.");
            default -> { /* AWAITING_PAYMENT, REJECTED and EXPIRED can all submit */ }
        }

        String filename = storage.store(screenshot);
        enrollment.submitProof(upiReference.trim(), filename, storage.contentTypeOf(screenshot));
        log.info("{} submitted payment proof for plan '{}'",
                student.getEmail(), enrollment.getPlan().getName());
        return PlanEnrollmentVo.from(enrollment);
    }

    @Transactional
    @Override
    public PlanEnrollmentVo cancel(Long enrollmentId, User student) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);
        if (!enrollment.isOwnedBy(student)) {
            throw new ForbiddenException("That isn't your purchase");
        }
        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            throw new ConflictException(
                    "This plan is already running. Contact an admin if you need a refund.");
        }
        enrollment.cancel();
        return PlanEnrollmentVo.from(enrollment);
    }

    // ---------- admin ----------

    @Override
    public List<PlanEnrollmentVo> awaitingReview() {
        return enrollmentRepository.findByStatusOrderBySubmittedAtAsc(EnrollmentStatus.SUBMITTED)
                .stream().map(PlanEnrollmentVo::from).toList();
    }

    @Override
    public List<PlanEnrollmentVo> all() {
        return enrollmentRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(PlanEnrollmentVo::from).toList();
    }

    /** Admin confirms the money arrived. This is what grants access. */
    @Transactional
    @Override
    public PlanEnrollmentVo activate(Long enrollmentId, User admin) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);
        if (enrollment.getStatus() != EnrollmentStatus.SUBMITTED) {
            throw new ConflictException("Only a SUBMITTED purchase can be confirmed. This one is "
                    + enrollment.getStatus() + ".");
        }
        enrollment.activate(admin);
        log.info("Plan '{}' activated for {} until {}",
                enrollment.getPlan().getName(),
                enrollment.getStudent().getEmail(),
                enrollment.getExpiresAt());
        return PlanEnrollmentVo.from(enrollment);
    }

    @Transactional
    @Override
    public PlanEnrollmentVo reject(Long enrollmentId, String reason, User admin) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);
        if (enrollment.getStatus() != EnrollmentStatus.SUBMITTED) {
            throw new ConflictException("Only a SUBMITTED purchase can be rejected. This one is "
                    + enrollment.getStatus() + ".");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Tell the student why, so they can fix it");
        }
        enrollment.reject(admin, reason.trim());
        return PlanEnrollmentVo.from(enrollment);
    }

    @Override
    public long countAwaitingReview() {
        return enrollmentRepository.countByStatus(EnrollmentStatus.SUBMITTED);
    }

    @Override
    public long countActive() {
        return enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE);
    }

    // ---------- screenshot ----------

    /** A picture of somebody's banking app: owner and admins only. */
    @Override
    public Path screenshotPath(Long enrollmentId, User caller) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);
        assertCanSee(enrollment, caller);

        if (enrollment.getScreenshotFile() == null) {
            throw new NotFoundException("No screenshot uploaded for this purchase");
        }

        Path path = storage.pathOf(enrollment.getScreenshotFile());

        // Checked before the controller starts streaming: once the 200 and its
        // headers are on the wire the status can no longer be changed, so a file
        // that has gone missing would give the client a truncated body instead
        // of an error it can act on.
        if (!Files.isReadable(path)) {
            throw new NotFoundException("That screenshot is no longer available");
        }
        return path;
    }

    @Override
    public String screenshotContentType(Long enrollmentId) {
        return getOrThrow(enrollmentId).getScreenshotContentType();
    }

    /** Plan ids this student currently holds. Used to gate study material. */
    @Override
    public List<Long> activePlanIds(User student) {
        return enrollmentRepository.findActivePlanIds(student.getId(), LocalDateTime.now());
    }

    // ---------- gateway settlement ----------

    @Override
    public PaymentPurpose purpose() {
        return PaymentPurpose.PLAN;
    }

    /**
     * pricePaid, not plan.getPrice().
     *
     * The enrollment froze the price when the student chose the plan. If an
     * admin raises the price while the student is on the payment screen, the
     * charge must still be the number they were shown - reading it live off the
     * plan would quietly move the goalposts mid-purchase.
     */
    @Override
    public PurchaseSettlement.Payable prepare(Long enrollmentId, User caller) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);

        if (!enrollment.isOwnedBy(caller)) {
            throw new ForbiddenException("That isn't your purchase");
        }
        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            throw new ConflictException("You already have this plan.");
        }
        if (enrollment.getStatus() == EnrollmentStatus.SUBMITTED) {
            throw new ConflictException(
                    "We're already checking the screenshot you sent. Wait for that to be "
                            + "reviewed rather than paying twice.");
        }
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new ConflictException("You cancelled this purchase. Choose the plan again to restart it.");
        }

        return new PurchaseSettlement.Payable(
                enrollment.getPricePaid(),
                enrollment.getPlan().getName() + " - " + enrollment.getDurationDays() + " days");
    }

    @Transactional
    @Override
    public void settle(Long enrollmentId, String gatewayPaymentId) {
        PlanEnrollment enrollment = getOrThrow(enrollmentId);
        enrollment.settleByGateway(gatewayPaymentId);

        log.info("Plan '{}' activated for {} by gateway payment {} - runs until {}",
                enrollment.getPlan().getName(), enrollment.getStudent().getEmail(),
                gatewayPaymentId, enrollment.getExpiresAt());
    }

    private void assertCanSee(PlanEnrollment enrollment, User caller) {
        if (caller.getRole() != Role.ADMIN && !enrollment.isOwnedBy(caller)) {
            throw new ForbiddenException("That isn't your purchase");
        }
    }

    private PlanEnrollment getOrThrow(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No purchase with id " + id));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.payment.PaymentDto;
import com.learn.interviewmentor.dto.payment.PaymentInstructionsDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.Payment;
import com.learn.interviewmentor.model.PaymentStatus;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.PaymentRepository;
import com.learn.interviewmentor.storage.ScreenshotStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Manual UPI payments, verified by a human.
 *
 * There is no gateway in this version. The student pays our UPI ID from their
 * own app, uploads a screenshot with the UTR, and an admin confirms the money
 * actually landed before the interview enters the mentor queue.
 *
 * The amount always comes from server config - it is never read from the
 * request body. A client that could name its own price would be the most
 * obvious hole in a payment flow.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ScreenshotStorage storage;

    private final String upiId;
    private final String payeeName;
    private final BigDecimal amount;

    public PaymentService(PaymentRepository paymentRepository,
                          ScreenshotStorage storage,
                          @Value("${app.payment.upi-id}") String upiId,
                          @Value("${app.payment.payee-name}") String payeeName,
                          @Value("${app.payment.amount}") BigDecimal amount) {
        this.paymentRepository = paymentRepository;
        this.storage = storage;
        this.upiId = upiId;
        this.payeeName = payeeName;
        this.amount = amount;
    }

    /** The price the server charges. Callers never get to suggest one. */
    public BigDecimal currentAmount() {
        return amount;
    }

    public PaymentInstructionsDto instructions() {
        String link = "upi://pay"
                + "?pa=" + enc(upiId)
                + "&pn=" + enc(payeeName)
                + "&am=" + amount.toPlainString()
                + "&cu=INR";
        return new PaymentInstructionsDto(upiId, payeeName, amount, "INR", link);
    }

    /** Created together with the request, in AWAITING. */
    @Transactional
    public Payment createFor(InterviewRequest request) {
        return paymentRepository.save(new Payment(request, amount));
    }

    public PaymentDto forRequest(Long requestId, User caller) {
        Payment payment = paymentRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("No payment for request " + requestId));
        assertCanSee(payment, caller);
        return PaymentDto.from(payment);
    }

    /**
     * Student uploads their proof. Also used to resubmit after a rejection.
     */
    @Transactional
    public PaymentDto submitProof(Long requestId, String upiReference, MultipartFile screenshot, User student) {
        Payment payment = paymentRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("No payment for request " + requestId));

        if (!payment.getRequest().isOwnedBy(student)) {
            throw new ForbiddenException("That isn't your booking");
        }
        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            throw new BadRequestException("This booking is already paid for.");
        }
        if (payment.getStatus() == PaymentStatus.SUBMITTED) {
            throw new BadRequestException("We're already checking your last screenshot.");
        }

        String filename = storage.store(screenshot);
        payment.submitProof(upiReference.trim(), filename, storage.contentTypeOf(screenshot));
        return PaymentDto.from(payment);
    }

    // ---------- admin ----------

    public List<PaymentDto> awaitingReview() {
        return paymentRepository.findByStatusOrderBySubmittedAtAsc(PaymentStatus.SUBMITTED)
                .stream().map(PaymentDto::from).toList();
    }

    /**
     * Admin confirms the money arrived. This is what releases the request into
     * the mentor queue - until now it was AWAITING_PAYMENT and invisible to them.
     */
    @Transactional
    public PaymentDto verify(Long paymentId, User admin) {
        Payment payment = getOrThrow(paymentId);
        if (payment.getStatus() != PaymentStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Only a SUBMITTED payment can be verified. This one is " + payment.getStatus() + ".");
        }

        payment.verify(admin);
        payment.getRequest().markPaid();
        return PaymentDto.from(payment);
    }

    @Transactional
    public PaymentDto reject(Long paymentId, String reason, User admin) {
        Payment payment = getOrThrow(paymentId);
        if (payment.getStatus() != PaymentStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Only a SUBMITTED payment can be rejected. This one is " + payment.getStatus() + ".");
        }
        payment.reject(admin, reason);
        return PaymentDto.from(payment);
    }

    // ---------- screenshot ----------

    /**
     * A screenshot of somebody's banking app is private, so only the student who
     * uploaded it and an admin may fetch it.
     */
    public Path screenshotPath(Long paymentId, User caller) {
        Payment payment = getOrThrow(paymentId);
        assertCanSee(payment, caller);

        if (payment.getScreenshotFile() == null) {
            throw new NotFoundException("No screenshot uploaded for this payment");
        }
        return storage.pathOf(payment.getScreenshotFile());
    }

    public String screenshotContentType(Long paymentId) {
        return getOrThrow(paymentId).getScreenshotContentType();
    }

    private void assertCanSee(Payment payment, User caller) {
        boolean allowed = caller.getRole() == Role.ADMIN || payment.getRequest().isOwnedBy(caller);
        if (!allowed) {
            throw new ForbiddenException("You cannot view this payment");
        }
    }

    private Payment getOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found with id " + id));
    }

    public long countAwaitingReview() {
        return paymentRepository.countByStatus(PaymentStatus.SUBMITTED);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

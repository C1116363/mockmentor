package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.payment.PaymentVo;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
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
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
public interface PaymentService {

    /** The price the server charges. Callers never get to suggest one. */
    BigDecimal currentAmount();

    PaymentInstructionsVo instructions();

    /** Created together with the request, in AWAITING. */
    Payment createFor(InterviewRequest request);

    PaymentVo forRequest(Long requestId, User caller);

    /**
    * Student uploads their proof. Also used to resubmit after a rejection.
    */
    PaymentVo submitProof(Long requestId, String upiReference, MultipartFile screenshot, User student);

    List<PaymentVo> awaitingReview();

    /**
    * Admin confirms the money arrived. This is what releases the request into
    * the mentor queue - until now it was AWAITING_PAYMENT and invisible to them.
    */
    PaymentVo verify(Long paymentId, User admin);

    PaymentVo reject(Long paymentId, String reason, User admin);

    /**
    * A screenshot of somebody's banking app is private, so only the student who
    * uploaded it and an admin may fetch it.
    */
    Path screenshotPath(Long paymentId, User caller);

    String screenshotContentType(Long paymentId);

    long countAwaitingReview();
}

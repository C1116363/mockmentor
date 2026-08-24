package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.payment.PaymentVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/** Paying for one booked session, by manual UPI. */
public interface PaymentFacade {

    ApiResult<PaymentInstructionsVo> instructions();

    ApiResult<PaymentVo> forRequest(Long requestId, User caller);

    ApiResult<PaymentVo> submitProof(Long requestId, String upiReference,
                                     MultipartFile screenshot, User student);

    /** Bytes, not an envelope - see PlanFacade#screenshotPath for why. */
    Path screenshotPath(Long paymentId, User caller);

    String screenshotContentType(Long paymentId);
}

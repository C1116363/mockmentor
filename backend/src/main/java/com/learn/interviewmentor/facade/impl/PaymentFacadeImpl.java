package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.facade.PaymentFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.PaymentService;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.payment.PaymentVo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Component
public class PaymentFacadeImpl implements PaymentFacade {

    private final PaymentService paymentService;

    public PaymentFacadeImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public ApiResult<PaymentInstructionsVo> instructions() {
        return ApiResult.ok(paymentService.instructions());
    }

    @Override
    public ApiResult<PaymentVo> forRequest(Long requestId, User caller) {
        return ApiResult.ok(paymentService.forRequest(requestId, caller));
    }

    @Override
    public ApiResult<PaymentVo> submitProof(Long requestId, String upiReference,
                                            MultipartFile screenshot, User student) {
        PaymentVo payment = paymentService.submitProof(requestId, upiReference, screenshot, student);
        return ApiResult.ok(payment,
                "Thanks - we're checking your payment. Your slot is held in the meantime.");
    }

    @Override
    public Path screenshotPath(Long paymentId, User caller) {
        return paymentService.screenshotPath(paymentId, caller);
    }

    @Override
    public String screenshotContentType(Long paymentId) {
        return paymentService.screenshotContentType(paymentId);
    }
}

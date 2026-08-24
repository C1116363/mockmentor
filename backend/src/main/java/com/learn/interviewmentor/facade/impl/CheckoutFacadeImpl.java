package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.checkout.CheckoutCallbackDto;
import com.learn.interviewmentor.facade.CheckoutFacade;
import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.CheckoutService;
import com.learn.interviewmentor.vo.checkout.CheckoutOptionsVo;
import com.learn.interviewmentor.vo.checkout.CheckoutResultVo;
import com.learn.interviewmentor.vo.checkout.CheckoutVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CheckoutFacadeImpl implements CheckoutFacade {

    private static final Logger log = LoggerFactory.getLogger(CheckoutFacadeImpl.class);

    private final CheckoutService checkoutService;

    public CheckoutFacadeImpl(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Override
    public ApiResult<CheckoutOptionsVo> options() {
        return ApiResult.ok(checkoutService.options());
    }

    @Override
    public ApiResult<CheckoutVo> start(PaymentPurpose purpose, Long targetId, User caller) {
        CheckoutVo checkout = checkoutService.start(purpose, targetId, caller);
        return ApiResult.created(checkout, "Checkout ready");
    }

    @Override
    public ApiResult<CheckoutResultVo> confirm(CheckoutCallbackDto callback, User caller) {
        CheckoutResultVo result = checkoutService.confirm(callback, caller);
        return ApiResult.ok(result, result.message());
    }

    /**
     * A duplicate delivery is a success, not an error.
     *
     * The unique constraint on the event id is what makes settlement happen
     * once, so hitting it means the gateway is retrying something we already
     * handled. Letting that become a 500 would tell Razorpay to keep retrying a
     * webhook that has already done its job - forever, on a schedule that backs
     * off but never stops.
     *
     * Caught here rather than inside the service because the service method is
     * the transaction: by the time the exception escapes it, the rollback has
     * happened and there is a clean answer to give.
     */
    @Override
    public String webhook(String rawBody, String signature, String eventId) {
        try {
            return checkoutService.handleWebhook(rawBody, signature, eventId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Webhook {} was a duplicate delivery - already handled", eventId);
            return "duplicate - already handled";
        }
    }
}

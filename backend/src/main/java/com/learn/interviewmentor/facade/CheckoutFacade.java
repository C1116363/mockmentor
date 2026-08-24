package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.checkout.CheckoutCallbackDto;
import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.checkout.CheckoutOptionsVo;
import com.learn.interviewmentor.vo.checkout.CheckoutResultVo;
import com.learn.interviewmentor.vo.checkout.CheckoutVo;

/** Paying by card, netbanking or UPI through a gateway. */
public interface CheckoutFacade {

    ApiResult<CheckoutOptionsVo> options();

    ApiResult<CheckoutVo> start(PaymentPurpose purpose, Long targetId, User caller);

    ApiResult<CheckoutResultVo> confirm(CheckoutCallbackDto callback, User caller);

    /**
     * The webhook.
     *
     * Returns a bare String rather than an ApiResult: the caller is Razorpay's
     * server, not our frontend, and it reads nothing but the status code. An
     * envelope here would be ceremony for an audience of none.
     */
    String webhook(String rawBody, String signature, String eventId);
}

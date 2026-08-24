package com.learn.interviewmentor.exception;

/**
 * The payment gateway is unreachable, misconfigured, or answered with something
 * we cannot use. Mapped to HTTP 502.
 *
 * <h2>Why 502 and not 500</h2>
 * 502 says "the thing upstream of me broke". That distinction is worth keeping:
 * a 500 sends somebody reading our code, a 502 sends them to the gateway's
 * status page - which is where the problem actually is most of the time.
 *
 * <h2>Why this is not a BadRequestException</h2>
 * The student did nothing wrong. Telling them "bad request" invites them to
 * retry the same correct thing, and the second attempt is the dangerous one:
 * money can move on a call we failed to read the answer to. The honest message
 * is "we could not reach the payment gateway - your money has not been taken".
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}

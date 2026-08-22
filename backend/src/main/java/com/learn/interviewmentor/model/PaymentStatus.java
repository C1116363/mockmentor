package com.learn.interviewmentor.model;

/**
 * Manual UPI payment, verified by a human.
 *
 * AWAITING  -> slot held, student hasn't sent proof yet
 * SUBMITTED -> student sent a UPI reference + screenshot, admin needs to check
 * VERIFIED  -> admin confirmed the money arrived; the interview enters the queue
 * REJECTED  -> admin couldn't match it; the student can submit again
 */
public enum PaymentStatus {
    AWAITING,
    SUBMITTED,
    VERIFIED,
    REJECTED
}

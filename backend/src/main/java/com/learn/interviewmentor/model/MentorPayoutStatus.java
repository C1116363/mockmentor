package com.learn.interviewmentor.model;

/**
 * PENDING   -> the work is counted and the amount is fixed; the money has not moved
 * PAID      -> an admin has sent it and recorded a reference
 * CANCELLED -> raised in error; the sessions it covered go back in the pot
 *
 * <h2>There is no DRAFT</h2>
 * A payout is only created once the numbers are settled, and creating it is
 * what stamps the sessions as covered. A draft state would mean sessions held
 * in limbo - not payable, not paid - and somebody would eventually have to
 * remember to go back and finish it.
 */
public enum MentorPayoutStatus {
    PENDING,
    PAID,
    CANCELLED
}

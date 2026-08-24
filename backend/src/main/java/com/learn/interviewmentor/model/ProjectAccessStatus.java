package com.learn.interviewmentor.model;

/**
 * A student's journey to being a collaborator on one project.
 *
 * AWAITING_PAYMENT -> requested, hasn't paid yet
 * SUBMITTED        -> UPI reference + screenshot sent, admin needs to check
 * ACTIVE           -> paid, and added as a collaborator on the repo
 * REJECTED         -> admin couldn't match the payment; they can resend
 * CANCELLED        -> student backed out before paying
 * EXPIRED          -> the access window ran out
 * REVOKED          -> an admin took access away early
 *
 * REVOKED is separate from EXPIRED on purpose. Both mean "no longer a
 * collaborator", but only one of them is a decision somebody made - and when a
 * student asks why they lost access, that difference is the whole answer.
 */
public enum ProjectAccessStatus {
    AWAITING_PAYMENT,
    SUBMITTED,
    ACTIVE,
    REJECTED,
    CANCELLED,
    EXPIRED,
    REVOKED
}

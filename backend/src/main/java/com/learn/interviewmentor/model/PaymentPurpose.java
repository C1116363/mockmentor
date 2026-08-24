package com.learn.interviewmentor.model;

/**
 * What a gateway payment is buying.
 *
 * The three things this app charges for, each already modelled by its own
 * entity with its own status flow. This enum plus a row id is what turns
 * "money arrived" into "activate that specific thing", and it is what a webhook
 * carries in its receipt - so the names here are effectively a wire format.
 * Renaming a constant orphans every in-flight payment that already quoted it.
 */
public enum PaymentPurpose {

    /** A mock interview or mentoring session. Settles a {@link Payment}. */
    INTERVIEW,

    /** A study plan. Settles a {@link PlanEnrollment}. */
    PLAN,

    /** Contributor access to a private repo. Settles a {@link ProjectAccessRequest}. */
    PROJECT
}

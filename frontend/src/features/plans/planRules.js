/**
 * Pure rules about plans and their purchases. No React, no fetch.
 *
 * Backend counterpart: the service layer.
 */

/** A purchase that is either in progress or currently granting access. */
const LIVE_ENROLLMENT = ["AWAITING_PAYMENT", "SUBMITTED", "ACTIVE", "REJECTED"];

/**
 * The enrollment that decides what a plan card offers.
 *
 * REJECTED counts as live on purpose: the student still has a purchase attached
 * to that plan and the card should offer "send new proof", not "get this plan"
 * as if nothing had happened.
 */
export function enrollmentFor(enrollments, planId) {
  return enrollments.find((e) => e.planId === planId && LIVE_ENROLLMENT.includes(e.status));
}

/** What the card's button should do. One place, so the card has no branching logic of its own. */
export function planCardState(enrollment) {
  if (!enrollment) return "BUY";
  if (enrollment.currentlyActive) return "OWNED";
  switch (enrollment.status) {
    case "SUBMITTED": return "CHECKING";
    case "REJECTED": return "REJECTED";
    case "AWAITING_PAYMENT": return "UNPAID";
    default: return "BUY";
  }
}

export const activePlanCount = (enrollments) => enrollments.filter((e) => e.currentlyActive).length;

export const needsPaymentCount = (enrollments) =>
  enrollments.filter((e) => ["AWAITING_PAYMENT", "REJECTED"].includes(e.status)).length;

/** Indian digit grouping: 2999 -> "2,999". Used wherever a price is printed. */
export const formatPrice = (value) => Number(value).toLocaleString("en-IN");

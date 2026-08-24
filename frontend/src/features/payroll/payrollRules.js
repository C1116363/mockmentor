/**
 * Payroll rules with no React and no fetch in them.
 *
 * Pure functions, so the awkward cases can be reasoned about on their own -
 * these are decisions about somebody's wages, and "what does the screen do when
 * a mentor has work but no rate" should not be buried inside a component.
 */

/** 2600 -> "2,600". Indian grouping, same as everywhere else. */
export const money = (value) => Number(value ?? 0).toLocaleString("en-IN");

/**
 * Why this mentor cannot be paid right now, or null if they can.
 *
 * Returns the single most useful reason rather than a list. An admin fixes one
 * thing and looks again; three simultaneous complaints just make the row noisy.
 * The order is deliberate - it matches the order the problems have to be fixed
 * in, so following it never leaves you stuck.
 */
export function blockedReason(mentor) {
  if (!mentor.payrollEnabled) return "Payroll is off for this mentor";
  if (mentor.interviewRate == null || mentor.mentoringRate == null) return "Set both rates first";
  if (mentor.hasPendingPayout) return "A payout is already waiting to be paid";
  if (mentor.unpaidInterviews + mentor.unpaidMentoring === 0) return "No completed sessions to pay for";
  return null;
}

export const canCreatePayout = (mentor) => blockedReason(mentor) === null;

/**
 * Worth warning about, but never worth blocking on.
 *
 * Missing bank details do not stop a payout being raised - the amount owed is a
 * fact about work already done, and refusing to record it because a form is
 * incomplete would be the app losing track of a real debt. So this is a warning
 * next to the button, not a disabled button.
 */
export const missingBankDetails = (mentor) =>
  mentor.unpaidInterviews + mentor.unpaidMentoring > 0 && !mentor.bankDetailsComplete;

/**
 * Mentors first if somebody is owed money, then by amount.
 *
 * The screen exists to answer "who needs paying", so the people who do are at
 * the top. Everyone else stays on the list - a mentor with nothing outstanding
 * is exactly who you look for when you want to check their rate.
 */
export function sortForPayroll(mentors) {
  return [...mentors].sort((a, b) => {
    const aOwed = a.unpaidInterviews + a.unpaidMentoring > 0;
    const bOwed = b.unpaidInterviews + b.unpaidMentoring > 0;
    if (aOwed !== bOwed) return aOwed ? -1 : 1;
    if (Number(b.amountDue) !== Number(a.amountDue)) return Number(b.amountDue) - Number(a.amountDue);
    return a.mentorName.localeCompare(b.mentorName);
  });
}

const STATUS_TONE = { PENDING: "warning", PAID: "success", CANCELLED: "muted" };
export const payoutTone = (status) => STATUS_TONE[status] ?? "muted";

/**
 * Pure rules about bookings. No React, no fetch - just functions over data.
 *
 * Backend counterpart: the service layer. These are the decisions ("is this
 * booking still live?", "which ones are coming up?") kept out of both the screen
 * and the data layer, so they can be read - and tested - on their own.
 */

/** Still in play. Drives the "My interviews" list. */
export const LIVE_STATUSES = ["AWAITING_PAYMENT", "PENDING", "SCHEDULED"];

/** Finished, one way or the other. Drives "History". */
export const DONE_STATUSES = ["COMPLETED", "CANCELLED"];

export const isLive = (session) => LIVE_STATUSES.includes(session.status);
export const isDone = (session) => DONE_STATUSES.includes(session.status);
export const isUnpaid = (session) => session.status === "AWAITING_PAYMENT";
export const isMentoring = (session) => session.sessionType === "MENTORING";

/** A mentoring session has no ratings, so it gets notes rather than a scorecard. */
export const isScored = (session) => session.scored !== false;

/**
 * Scheduled, and not finished more than two hours ago. Soonest first.
 *
 * The cutoff is deliberately in the past: a call that started an hour ago is the
 * one you most need the join link for, and dropping it the moment the clock ticks
 * over would hide it exactly when it matters.
 *
 * No limit - the strip shows everything that qualifies, and the count in its
 * heading is only right if nothing has been trimmed.
 */
export function selectUpcoming(sessions, { graceHours = 2 } = {}) {
  const cutoff = Date.now() - graceHours * 60 * 60 * 1000;
  return sessions
    .filter((s) => s.status === "SCHEDULED" && s.scheduledAt && new Date(s.scheduledAt) > cutoff)
    .sort((a, b) => new Date(a.scheduledAt) - new Date(b.scheduledAt));
}

/** Split one list into the two tabs, in a single pass. */
export function partitionSessions(sessions) {
  const live = sessions.filter(isLive);
  const done = sessions.filter(isDone);
  return { live, done, unpaidCount: live.filter(isUnpaid).length };
}

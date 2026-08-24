/**
 * Pure rules about live projects and contributor access. No React, no fetch.
 *
 * Backend counterpart: the service layer.
 */

/** A request that is in progress or currently granting access. */
const LIVE_ACCESS = ["AWAITING_PAYMENT", "SUBMITTED", "ACTIVE", "REJECTED"];

export function accessFor(requests, projectId) {
  return requests.find((r) => r.projectId === projectId && LIVE_ACCESS.includes(r.status));
}

/**
 * What the card's button should offer.
 *
 * GRANTED and INVITE_PENDING are deliberately different states. Both mean the
 * payment is confirmed, but only one means you can actually open the repo - and
 * telling somebody "you're in" when they will hit a 404 is worse than telling
 * them to wait.
 */
export function projectCardState(access, project) {
  if (!access) return project.seatsAvailable ? "REQUEST" : "FULL";
  if (access.currentlyActive) {
    return access.collaboratorGranted ? "GRANTED" : "INVITE_PENDING";
  }
  switch (access.status) {
    case "SUBMITTED": return "CHECKING";
    case "REJECTED": return "REJECTED";
    case "AWAITING_PAYMENT": return "UNPAID";
    default: return project.seatsAvailable ? "REQUEST" : "FULL";
  }
}

export const activeAccessCount = (requests) => requests.filter((r) => r.currentlyActive).length;

export const needsPaymentCount = (requests) =>
  requests.filter((r) => ["AWAITING_PAYMENT", "REJECTED"].includes(r.status)).length;

/** Access that is live but still waiting on the GitHub invite. */
export const awaitingInviteCount = (requests) =>
  requests.filter((r) => r.currentlyActive && !r.collaboratorGranted).length;

export const DIFFICULTY_CLASS = {
  BEGINNER: "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED: "advanced",
};

/** "in 62 days", "today", "8 days ago" - for an access window. */
export function daysLeftLabel(expiresAt) {
  if (!expiresAt) return null;
  const days = Math.ceil((new Date(expiresAt) - Date.now()) / 86400000);
  if (days > 1) return `${days} days left`;
  if (days === 1) return "1 day left";
  if (days === 0) return "expires today";
  return `expired ${Math.abs(days)} day${Math.abs(days) === 1 ? "" : "s"} ago`;
}

/**
 * GitHub's own username rules, mirrored client-side.
 *
 * 1-39 characters, alphanumerics and single hyphens, no leading or trailing
 * hyphen. Checked here only so the student sees it immediately - the server
 * validates the same thing, because a bad handle means the invite 404s after
 * they have paid.
 */
const GITHUB_USERNAME = /^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$/;

export const isValidGithubUsername = (value) => GITHUB_USERNAME.test((value ?? "").trim());

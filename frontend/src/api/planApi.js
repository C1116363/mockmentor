import { request } from "./http";

/**
 * Backend: PlanController -> /api/plans (the student side of plans)
 *
 * Prices come from the database, so whatever an admin last set is what these
 * return. Nothing is cached here on purpose - a stale price on a buy button is
 * the one thing this feature must never do.
 */
export const planApi = {
  all: () => request("/plans"),

  one: (id) => request(`/plans/${id}`),

  /** Safe to call twice: the server hands back the purchase already in progress. */
  enroll: (planId) => request(`/plans/${planId}/enroll`, { method: "POST" }),

  myEnrollments: () => request("/plans/enrollments/mine"),

  /** The amount here is the price frozen when the purchase was created. */
  paymentInstructions: (enrollmentId) =>
    request(`/plans/enrollments/${enrollmentId}/instructions`),

  submitProof: (enrollmentId, upiReference, screenshot) => {
    const body = new FormData();
    body.append("upiReference", upiReference);
    body.append("screenshot", screenshot);
    return request(`/plans/enrollments/${enrollmentId}/proof`, { method: "POST", body });
  },

  cancelEnrollment: (enrollmentId) =>
    request(`/plans/enrollments/${enrollmentId}/cancel`, { method: "PATCH" }),
};

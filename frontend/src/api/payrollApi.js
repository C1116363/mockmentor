import { request, requestEnvelope } from "./http";

/** Backend: AdminPayrollController -> /api/admin/payroll (paying mentors) */
export const payrollApi = {
  mentors: () => request("/admin/payroll/mentors"),

  summary: () => request("/admin/payroll/summary"),

  /** Turn payroll on/off and set the two rates. */
  configure: (mentorId, settings) =>
    requestEnvelope(`/admin/payroll/mentors/${mentorId}/settings`, {
      method: "PATCH",
      body: JSON.stringify(settings),
    }),

  /**
   * Raise a payout for everything a mentor is owed.
   *
   * No body, and no amount - the server reads the rates off the mentor's
   * profile and counts the sessions itself. A client that could name the
   * amount could name anyone's wages.
   */
  createPayout: (mentorId) =>
    requestEnvelope(`/admin/payroll/mentors/${mentorId}/payouts`, { method: "POST" }),

  markPaid: (payoutId, paymentReference, notes) =>
    requestEnvelope(`/admin/payroll/payouts/${payoutId}/mark-paid`, {
      method: "PATCH",
      body: JSON.stringify({ paymentReference, notes }),
    }),

  cancelPayout: (payoutId, reason) =>
    requestEnvelope(
      `/admin/payroll/payouts/${payoutId}/cancel?reason=${encodeURIComponent(reason)}`,
      { method: "PATCH" }
    ),

  payouts: () => request("/admin/payroll/payouts"),

  payoutsFor: (mentorId) => request(`/admin/payroll/mentors/${mentorId}/payouts`),
};

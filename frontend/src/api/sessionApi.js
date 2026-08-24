import { request } from "./http";

/**
 * Backend: InterviewRequestController -> /api/requests
 *
 * "Session" rather than "interview": one booking is either a MOCK_INTERVIEW or a
 * MENTORING discussion, and the endpoints serve both.
 */
export const sessionApi = {
  /**
   * payload.sessionType is MOCK_INTERVIEW or MENTORING. Omitting it books an
   * interview, which is what every caller written before mentoring existed does.
   */
  create: (payload) => request("/requests", { method: "POST", body: JSON.stringify(payload) }),

  mine: () => request("/requests/mine"),

  cancel: (id) => request(`/requests/${id}/cancel`, { method: "PATCH" }),

  // ---- mentor side ----
  openQueue: () => request("/requests/pending"),
  assignedToMe: () => request("/requests/assigned"),

  accept: (id, payload) =>
    request(`/requests/${id}/accept`, { method: "PATCH", body: JSON.stringify(payload) }),

  complete: (id, payload) =>
    request(`/requests/${id}/complete`, { method: "PATCH", body: JSON.stringify(payload) }),
};

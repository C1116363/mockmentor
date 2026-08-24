import { request, requestEnvelope } from "./http";

/**
 * Backend: AdminController + AdminPlanController -> /api/admin
 *
 * Grouped by what the admin is looking at, in the same order as the dashboard
 * tabs, so a screen and its calls can be read side by side.
 */
export const adminApi = {
  stats: () => request("/admin/stats"),

  // ---- users ----
  users: () => request("/admin/users"),
  deactivateUser: (id) => request(`/admin/users/${id}/deactivate`, { method: "PATCH" }),
  activateUser: (id) => request(`/admin/users/${id}/activate`, { method: "PATCH" }),

  // ---- mentor verification ----
  pendingMentorProfiles: () => request("/admin/mentor-profiles/pending"),
  allMentorProfiles: () => request("/admin/mentor-profiles"),
  approveMentor: (id) => request(`/admin/mentor-profiles/${id}/approve`, { method: "PATCH" }),
  rejectMentor: (id, reason) =>
    request(`/admin/mentor-profiles/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ reason }),
    }),

  // ---- mentor availability (the reference view) ----
  availability: (from, days = 7) =>
    request(`/admin/availability?days=${days}` + (from ? `&from=${from}` : "")),
  /**
   * Who declared this booking's exact hour, for its exact session type.
   * The list to assign from - it means the mentor already agreed to the time.
   */
  availableMentorsFor: (requestId) => request(`/admin/requests/${requestId}/available-mentors`),

  // ---- sessions ----
  allRequests: () => request("/admin/requests"),
  unassignedRequests: () => request("/admin/requests/pending"),
  assignMentor: (requestId, payload) =>
    request(`/admin/requests/${requestId}/assign`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),

  // ---- interview payments ----
  pendingPayments: () => request("/admin/payments/pending"),
  verifyPayment: (id) => request(`/admin/payments/${id}/verify`, { method: "PATCH" }),
  rejectPayment: (id, reason) =>
    request(`/admin/payments/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ reason }),
    }),

  // ---- plans & prices ----
  plans: () => request("/admin/plans"),
  createPlan: (payload) => request("/admin/plans", { method: "POST", body: JSON.stringify(payload) }),
  updatePlan: (id, payload) =>
    request(`/admin/plans/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  /** Its own endpoint, so changing a price can never blank out a description. */
  // requestEnvelope: the server's message spells out the consequence ("students
  // see it on their next load"), which is the reason an admin came here.
  updatePlanPrice: (id, price) =>
    requestEnvelope(`/admin/plans/${id}/price`, {
      method: "PATCH", body: JSON.stringify({ price }),
    }),
  setPlanActive: (id, active) =>
    request(`/admin/plans/${id}/active?active=${active}`, { method: "PATCH" }),

  // ---- plan payments ----
  pendingEnrollments: () => request("/admin/plan-enrollments/pending"),
  allEnrollments: () => request("/admin/plan-enrollments"),
  activateEnrollment: (id) => request(`/admin/plan-enrollments/${id}/activate`, { method: "PATCH" }),
  rejectEnrollment: (id, reason) =>
    request(`/admin/plan-enrollments/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ reason }),
    }),

  // ---- study material ----
  materials: () => request("/admin/materials"),

  uploadMaterial: ({ title, description, file, targetStudentId, targetPlanId }) => {
    const body = new FormData();
    body.append("title", title);
    if (description) body.append("description", description);
    body.append("file", file);
    // At most one audience id - the backend rejects both at once, because a row
    // has exactly one audience.
    if (targetStudentId) body.append("targetStudentId", targetStudentId);
    else if (targetPlanId) body.append("targetPlanId", targetPlanId);
    return request("/admin/materials", { method: "POST", body });
  },

  shareMaterialLink: ({ title, description, linkUrl, targetStudentId, targetPlanId }) => {
    const params = new URLSearchParams();
    if (targetStudentId) params.set("targetStudentId", targetStudentId);
    else if (targetPlanId) params.set("targetPlanId", targetPlanId);
    const query = params.toString() ? `?${params}` : "";
    return request(`/admin/materials/link${query}`, {
      method: "POST",
      body: JSON.stringify({ title, description, linkUrl }),
    });
  },

  setMaterialActive: (id, active) =>
    request(`/admin/materials/${id}/active?active=${active}`, { method: "PATCH" }),
};

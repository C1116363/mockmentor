import { request, requestEnvelope } from "./http";

/**
 * Backend: AdminProjectController -> /api/admin/projects
 *
 * Its own module rather than more methods on adminApi, so the 1:1 mapping between
 * an api/ file and a backend controller holds.
 *
 * Note the three separate queues. They are not arbitrary: `pending` is about
 * money, `awaitingInvite` is people who have paid and still cannot see the code,
 * and `pastExpiry` is people who should have been removed already. Collapsing
 * them would hide the second two.
 */
export const adminProjectApi = {
  // ---- the catalogue ----
  all: () => request("/admin/projects"),
  create: (payload) => request("/admin/projects", { method: "POST", body: JSON.stringify(payload) }),
  update: (id, payload) =>
    request(`/admin/projects/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  updatePrice: (id, price) =>
    request(`/admin/projects/${id}/price`, { method: "PATCH", body: JSON.stringify({ price }) }),
  setActive: (id, active) =>
    request(`/admin/projects/${id}/active?active=${active}`, { method: "PATCH" }),
  contributors: (id) => request(`/admin/projects/${id}/contributors`),

  // ---- access requests ----
  pending: () => request("/admin/projects/access/pending"),
  /** Paid and approved, but nobody has added them on GitHub yet. */
  awaitingInvite: () => request("/admin/projects/access/awaiting-invite"),
  /** Access that has run out but is still granted on the repo. */
  pastExpiry: () => request("/admin/projects/access/past-expiry"),
  allAccess: () => request("/admin/projects/access"),

  /**
   * requestEnvelope, not request: with the manual provider the response message
   * carries the exact next step - "Add @user to owner/repo: <settings link>" -
   * and dropping it would leave an admin with nothing to act on.
   */
  approve: (id) => requestEnvelope(`/admin/projects/access/${id}/approve`, { method: "PATCH" }),
  /** Click once you have actually added them on GitHub. */
  confirmInvite: (id) =>
    requestEnvelope(`/admin/projects/access/${id}/confirm-invite`, { method: "PATCH" }),
  reject: (id, reason) =>
    request(`/admin/projects/access/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ reason }),
    }),
  revoke: (id, reason) =>
    requestEnvelope(`/admin/projects/access/${id}/revoke`, {
      method: "PATCH",
      body: JSON.stringify({ reason }),
    }),
};

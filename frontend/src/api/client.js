// All calls to the Spring Boot backend go through this one file.

const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";
const TOKEN_KEY = "mockmentor.token";

// The token lives in localStorage so a page refresh doesn't log you out.
// Note: localStorage is readable by any JS on the page, so it is vulnerable to
// XSS. Production apps often use an httpOnly cookie instead. Fine for learning.
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/** Carries the backend's error message (and field errors) up to the component. */
export class ApiError extends Error {
  constructor(body, status) {
    super(body?.message ?? "Something went wrong. Is the backend running?");
    this.status = status;
    this.fieldErrors = body?.fieldErrors ?? {};
  }
}

async function request(path, options = {}) {
  const token = tokenStore.get();

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      // This header is the whole authentication mechanism on the client side.
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  const body = response.status === 204 ? null : await response.json().catch(() => null);

  if (!response.ok) {
    // An expired or tampered token gives 401 - drop it so the app shows login.
    if (response.status === 401 && token) {
      tokenStore.clear();
    }
    throw new ApiError(body, response.status);
  }
  return body;
}

export const api = {
  // ---- auth ----
  login: (payload) => request("/auth/login", { method: "POST", body: JSON.stringify(payload) }),
  signupStudent: (payload) =>
    request("/auth/signup/student", { method: "POST", body: JSON.stringify(payload) }),
  signupMentor: (payload) =>
    request("/auth/signup/mentor", { method: "POST", body: JSON.stringify(payload) }),
  me: () => request("/auth/me"),

  // ---- interview requests ----
  createRequest: (payload) => request("/requests", { method: "POST", body: JSON.stringify(payload) }),
  myRequests: () => request("/requests/mine"),

  // ---- slots ----
  // date is "yyyy-MM-dd"; returns every 1-hour slot for that day with availability
  slots: (date) => request(`/slots?date=${encodeURIComponent(date)}`),

  cancelRequest: (id) => request(`/requests/${id}/cancel`, { method: "PATCH" }),

  // ---- mentor ----
  myMentorProfile: () => request("/mentor/profile"),
  submitMentorProfile: (payload) =>
    request("/mentor/profile", { method: "PUT", body: JSON.stringify(payload) }),
  pendingRequests: () => request("/requests/pending"),
  assignedRequests: () => request("/requests/assigned"),
  acceptRequest: (id, payload) =>
    request(`/requests/${id}/accept`, { method: "PATCH", body: JSON.stringify(payload) }),
  completeRequest: (id, payload) =>
    request(`/requests/${id}/complete`, { method: "PATCH", body: JSON.stringify(payload) }),

  // ---- admin ----
  adminStats: () => request("/admin/stats"),
  adminUsers: () => request("/admin/users"),
  adminRequests: () => request("/admin/requests"),
  adminPendingRequests: () => request("/admin/requests/pending"),
  assignMentor: (requestId, payload) =>
    request(`/admin/requests/${requestId}/assign`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),
  listMentors: () => request("/mentors"),
  pendingMentorProfiles: () => request("/admin/mentor-profiles/pending"),
  allMentorProfiles: () => request("/admin/mentor-profiles"),
  approveMentor: (id) => request(`/admin/mentor-profiles/${id}/approve`, { method: "PATCH" }),
  rejectMentor: (id, reason) =>
    request(`/admin/mentor-profiles/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ reason }),
    }),
  deactivateUser: (id) => request(`/admin/users/${id}/deactivate`, { method: "PATCH" }),
  activateUser: (id) => request(`/admin/users/${id}/activate`, { method: "PATCH" }),
};

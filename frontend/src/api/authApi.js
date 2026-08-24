import { request, requestEnvelope } from "./http";

/** Backend: AuthController -> /api/auth */
export const authApi = {
  login: (payload) => request("/auth/login", { method: "POST", body: JSON.stringify(payload) }),

  signupStudent: (payload) =>
    request("/auth/signup/student", { method: "POST", body: JSON.stringify(payload) }),

  signupMentor: (payload) =>
    request("/auth/signup/mentor", { method: "POST", body: JSON.stringify(payload) }),

  /** No id parameter - the server reads it off the token, so you only ever get yourself. */
  me: () => request("/auth/me"),

  /**
   * Ask for a reset link.
   *
   * Uses requestEnvelope rather than request because the *message* is the whole
   * answer here - the server deliberately returns no data, and says the same
   * thing whether or not the address has an account.
   */
  forgotPassword: (email) =>
    requestEnvelope("/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ email }),
    }),

  resetPassword: (token, newPassword) =>
    requestEnvelope("/auth/reset-password", {
      method: "POST",
      body: JSON.stringify({ token, newPassword }),
    }),
};

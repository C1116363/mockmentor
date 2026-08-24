import { request } from "./http";

/** Backend: AuthController -> /api/auth */
export const authApi = {
  login: (payload) => request("/auth/login", { method: "POST", body: JSON.stringify(payload) }),

  signupStudent: (payload) =>
    request("/auth/signup/student", { method: "POST", body: JSON.stringify(payload) }),

  signupMentor: (payload) =>
    request("/auth/signup/mentor", { method: "POST", body: JSON.stringify(payload) }),

  /** No id parameter - the server reads it off the token, so you only ever get yourself. */
  me: () => request("/auth/me"),
};

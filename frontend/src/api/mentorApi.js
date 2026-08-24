import { request } from "./http";

/** Backend: MentorController + MentorProfileController -> /api/mentors, /api/mentor/profile */
export const mentorApi = {
  /** Verified mentors only. Any logged-in user may read this. */
  all: () => request("/mentors"),

  myProfile: () => request("/mentor/profile"),

  submitProfile: (payload) =>
    request("/mentor/profile", { method: "PUT", body: JSON.stringify(payload) }),
};

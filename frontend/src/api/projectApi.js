import { request } from "./http";

/**
 * Backend: ProjectController -> /api/projects (the student side of live projects)
 *
 * One thing to know when reading responses from here: `repoFullName`, `repoUrl`
 * and `onboardingUrl` are **null unless your access to that project is active**.
 * The server withholds them - these are private repositories, and naming one to
 * somebody who cannot open it only tells an attacker what to aim at. So a UI that
 * wants to show a repo link must check `currentlyActive` first, not assume.
 */
export const projectApi = {
  all: () => request("/projects"),

  one: (id) => request(`/projects/${id}`),

  /**
   * githubUsername is required - it is what gets added to the repository.
   * Safe to call twice: the server returns the request already in progress.
   */
  requestAccess: (projectId, payload) =>
    request(`/projects/${projectId}/request-access`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  myAccess: () => request("/projects/access/mine"),

  paymentInstructions: (accessId) => request(`/projects/access/${accessId}/instructions`),

  submitProof: (accessId, upiReference, screenshot) => {
    const body = new FormData();
    body.append("upiReference", upiReference);
    body.append("screenshot", screenshot);
    return request(`/projects/access/${accessId}/proof`, { method: "POST", body });
  },

  /** Only works before access has been granted - 409 afterwards. */
  changeGithubUsername: (accessId, githubUsername) =>
    request(
      `/projects/access/${accessId}/github-username?githubUsername=${encodeURIComponent(githubUsername)}`,
      { method: "PATCH" }
    ),

  cancel: (accessId) => request(`/projects/access/${accessId}/cancel`, { method: "PATCH" }),
};

import { useCallback, useEffect, useState } from "react";
import { projectApi } from "../../api/projectApi";
import { accessFor, activeAccessCount, awaitingInviteCount, needsPaymentCount } from "./projectRules";

/**
 * The project catalogue plus this student's access requests.
 *
 * Both together, because "what can I contribute to" and "what do I already have"
 * are one question from the screen's point of view - and because the repository
 * path only appears in the catalogue for projects the caller already holds, so
 * the two lists have to be refreshed together or the card and the link disagree.
 */
export function useProjects() {
  const [projects, setProjects] = useState([]);
  const [access, setAccess] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    const [p, a] = await Promise.all([projectApi.all(), projectApi.myAccess()]);
    setProjects(p);
    setAccess(a);
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  /** Returns the request, so the caller can go straight into paying for it. */
  const requestAccess = useCallback(async (projectId, payload) => {
    const created = await projectApi.requestAccess(projectId, payload);
    await reload();
    return created;
  }, [reload]);

  const cancel = useCallback(async (accessId) => {
    await projectApi.cancel(accessId);
    await reload();
  }, [reload]);

  const changeGithubUsername = useCallback(async (accessId, username) => {
    await projectApi.changeGithubUsername(accessId, username);
    await reload();
  }, [reload]);

  return {
    projects,
    access,
    loading,
    error,
    reload,
    requestAccess,
    cancel,
    changeGithubUsername,
    /** Curried so a card asks about itself: accessOf(project.id) */
    accessOf: (projectId) => accessFor(access, projectId),
    activeCount: activeAccessCount(access),
    needsPaymentCount: needsPaymentCount(access),
    awaitingInviteCount: awaitingInviteCount(access),
  };
}

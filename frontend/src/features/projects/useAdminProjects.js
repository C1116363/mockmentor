import { useCallback, useEffect, useState } from "react";
import { adminProjectApi } from "../../api/adminProjectApi";

/**
 * The admin side of live projects: the catalogue and the three access queues.
 *
 * <b>Three queues, not one.</b> They answer different questions and collapsing
 * them would hide two of them:
 *
 *   pending        - money to check
 *   awaitingInvite - paid, approved, still not on the repo. Somebody is locked
 *                    out of what they bought.
 *   pastExpiry     - access ran out but they are still a collaborator. Nothing
 *                    sweeps this automatically.
 *
 * One reload for all of it, because the actions move rows between the queues:
 * approving takes a row out of `pending` and puts it into `awaitingInvite`, and
 * refreshing only one would leave the screen contradicting itself.
 */
export function useAdminProjects() {
  const [projects, setProjects] = useState([]);
  const [pending, setPending] = useState([]);
  const [awaitingInvite, setAwaitingInvite] = useState([]);
  const [pastExpiry, setPastExpiry] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const reload = useCallback(async () => {
    const [p, pen, inv, exp] = await Promise.all([
      adminProjectApi.all(),
      adminProjectApi.pending(),
      adminProjectApi.awaitingInvite(),
      adminProjectApi.pastExpiry(),
    ]);
    setProjects(p);
    setPending(pen);
    setAwaitingInvite(inv);
    setPastExpiry(exp);
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setMessage({ type: "error", text: e.message }))
      .finally(() => setLoading(false));
  }, [reload]);

  /**
   * Run, reload, report. Keeps fourteen actions from repeating the same try/catch.
   *
   * Actions that go through requestEnvelope resolve to `{ data, message }`, and
   * that message is passed straight through - the backend already writes the
   * sentence worth showing, including the GitHub next step, and a second one
   * written here could only disagree with it.
   */
  const run = useCallback(async (action, fallbackText) => {
    try {
      const result = await action();
      await reload();
      const message = result && typeof result === "object" && "message" in result
        ? result.message
        : null;
      setMessage({ type: "success", text: message ?? fallbackText });
      return result;
    } catch (e) {
      setMessage({ type: "error", text: e.message });
      throw e;
    }
  }, [reload]);

  return {
    projects,
    pending,
    awaitingInvite,
    pastExpiry,
    loading,
    message,
    setMessage,
    reload,

    // ---- the catalogue ----
    // Not wrapped in run(): the inline price form shows its own error next to the
    // input, so the throw has to reach it.
    savePrice: async (project, price) => {
      const updated = await adminProjectApi.updatePrice(project.id, price);
      await reload();
      setMessage({ type: "success", text: `${project.name} is now ₹${price}.` });
      return updated;
    },
    saveProject: (target, payload) => run(
      () => (target === "new" ? adminProjectApi.create(payload)
                              : adminProjectApi.update(target.id, payload)),
      "Project saved."),
    toggleProject: (project) => run(
      () => adminProjectApi.setActive(project.id, !project.active),
      project.active ? `${project.name} closed.` : `${project.name} opened.`),
    contributors: (projectId) => adminProjectApi.contributors(projectId),

    // ---- access ----
    approve: (access) => run(() => adminProjectApi.approve(access.id),
      `Approved ${access.studentName}.`),
    confirmInvite: (access) => run(() => adminProjectApi.confirmInvite(access.id),
      `@${access.githubUsername} confirmed.`),
    reject: (access, reason) => run(() => adminProjectApi.reject(access.id, reason),
      "Rejected."),
    revoke: (access, reason) => run(() => adminProjectApi.revoke(access.id, reason),
      "Access revoked."),
  };
}

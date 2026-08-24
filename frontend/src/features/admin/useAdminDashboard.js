import { useCallback, useEffect, useState } from "react";
import { adminApi } from "../../api/adminApi";
import { mentorApi } from "../../api/mentorApi";

/**
 * Everything the admin dashboard shows, and every action it can take.
 *
 * Backend counterpart: AdminFacade - and for the same reason. One admin screen
 * genuinely needs users, mentor profiles, sessions, two payment queues, plans and
 * material all at once, so something has to compose them. Better here, once, than
 * sixteen useState calls in the component.
 *
 * One reload for the whole screen is deliberate. Verifying a payment moves a
 * booking into the mentor queue and changes three stat tiles; refreshing only the
 * payments list would leave the rest of the screen quietly wrong.
 */
export function useAdminDashboard() {
  const [data, setData] = useState({
    users: [],
    profiles: [],
    unassigned: [],
    requests: [],
    mentors: [],
    payments: [],
    plans: [],
    planPayments: [],
    materials: [],
  });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const reload = useCallback(async () => {
    // No stats call: the tiles it fed are gone, and the tabs already carry every
    // queue count. One fewer request on every load and every action's reload.
    const [profiles, unassigned, mentors, users, requests,
           payments, plans, planPayments, materials] = await Promise.all([
      adminApi.allMentorProfiles(),
      adminApi.unassignedRequests(),
      mentorApi.all(),
      adminApi.users(),
      adminApi.allRequests(),
      adminApi.pendingPayments(),
      adminApi.plans(),
      adminApi.pendingEnrollments(),
      adminApi.materials(),
    ]);
    setData({ profiles, unassigned, mentors, users, requests,
              payments, plans, planPayments, materials });
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setMessage({ type: "error", text: e.message }))
      .finally(() => setLoading(false));
  }, [reload]);

  /**
   * Every action goes through here: run it, reload, report.
   *
   * Without this each of the fourteen actions below would repeat the same
   * try/catch/reload, and the day one of them forgot the reload the screen would
   * silently show stale data.
   */
  const run = useCallback(async (action, successText) => {
    try {
      const result = await action();
      await reload();
      if (successText) {
        setMessage({ type: "success", text: typeof successText === "function"
          ? successText(result) : successText });
      }
      return result;
    } catch (e) {
      setMessage({ type: "error", text: e.message });
      throw e;
    }
  }, [reload]);

  return {
    ...data,
    loading,
    message,
    setMessage,
    reload,

    // The backend already returns a sentence worth showing for most of these,
    // so where it does we pass its message straight through rather than writing
    // a second one that could disagree with it.
    verifyPayment: (p) => run(() => adminApi.verifyPayment(p.id),
      `Payment confirmed. ${p.studentName}'s session is now open to mentors.`),
    rejectPayment: (p, reason) => run(() => adminApi.rejectPayment(p.id, reason),
      "Payment rejected. The student can send new proof."),

    approveMentor: (profile) => run(() => adminApi.approveMentor(profile.id),
      `${profile.fullName} is now verified.`),
    rejectMentor: (profile, reason) => run(() => adminApi.rejectMentor(profile.id, reason),
      `${profile.fullName} was rejected.`),

    assignMentor: (requestId, payload) => run(() => adminApi.assignMentor(requestId, payload),
      (r) => `Assigned to ${r.mentor.name}. The student can see it now.`),

    toggleUser: (user) => run(() => user.active
      ? adminApi.deactivateUser(user.id)
      : adminApi.activateUser(user.id)),

    // ---- plans ----
    // Not wrapped in run(): the price form shows its own error inline, next to
    // the input the admin just typed in, so the throw has to reach it.
    savePlanPrice: async (plan, price) => {
      // Envelope, not payload: the message is the point here.
      const { data, message } = await adminApi.updatePlanPrice(plan.id, price);
      await reload();
      setMessage({ type: "success", text: message ?? `${plan.name} updated.` });
      return data;
    },
    savePlan: (planOrNew, payload) => run(
      () => (planOrNew === "new" ? adminApi.createPlan(payload)
                                 : adminApi.updatePlan(planOrNew.id, payload)),
      (p) => `Saved ${p.name}.`),
    togglePlan: (plan) => run(() => adminApi.setPlanActive(plan.id, !plan.active),
      plan.active
        ? `${plan.name} is off sale. Students who already bought it keep their access.`
        : `${plan.name} is back on sale.`),

    // ---- plan payments ----
    activateEnrollment: (e) => run(() => adminApi.activateEnrollment(e.id),
      `${e.studentName} now has ${e.planName}.`),
    rejectEnrollment: (e, reason) => run(() => adminApi.rejectEnrollment(e.id, reason),
      "Rejected. The student can send new proof."),

    // ---- study material ----
    // These two return the saved row, because the send form prints its own
    // confirmation naming the audience.
    uploadMaterial: async (payload) => {
      const saved = await adminApi.uploadMaterial(payload);
      await reload();
      return saved;
    },
    shareMaterialLink: async (payload) => {
      const saved = await adminApi.shareMaterialLink(payload);
      await reload();
      return saved;
    },
    toggleMaterial: (m) => run(() => adminApi.setMaterialActive(m.id, !m.active)),
  };
}

import { useCallback, useEffect, useState } from "react";
import { planApi } from "../../api/planApi";
import { activePlanCount, enrollmentFor, needsPaymentCount } from "./planRules";

/**
 * The price list plus this student's purchases.
 *
 * Backend counterpart: the facade layer - and like PlanFacade, it holds the two
 * together because "what can I buy" and "what do I already own" are one question
 * from the screen's point of view.
 */
export function usePlans() {
  const [plans, setPlans] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    const [p, e] = await Promise.all([planApi.all(), planApi.myEnrollments()]);
    setPlans(p);
    setEnrollments(e);
  }, []);

  useEffect(() => {
    reload()
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [reload]);

  /** Returns the enrollment, so the caller can go straight into paying for it. */
  const enroll = useCallback(async (planId) => {
    const enrollment = await planApi.enroll(planId);
    await reload();
    return enrollment;
  }, [reload]);

  const cancel = useCallback(async (enrollmentId) => {
    await planApi.cancelEnrollment(enrollmentId);
    await reload();
  }, [reload]);

  return {
    plans,
    enrollments,
    loading,
    error,
    reload,
    enroll,
    cancel,
    /** Curried so a card asks about itself: enrollmentOf(plan.id) */
    enrollmentOf: (planId) => enrollmentFor(enrollments, planId),
    activeCount: activePlanCount(enrollments),
    needsPaymentCount: needsPaymentCount(enrollments),
  };
}

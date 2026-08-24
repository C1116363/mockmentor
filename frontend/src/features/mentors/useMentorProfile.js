import { useCallback, useEffect, useState } from "react";
import { mentorApi } from "../../api/mentorApi";

/**
 * A mentor's own onboarding profile - the one an admin verifies.
 *
 * Loads on mount so the form can be pre-filled when they come back to edit a
 * rejected submission rather than retyping everything.
 */
export function useMentorProfile({ load = true } = {}) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(load);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setProfile(await mentorApi.myProfile());
  }, []);

  useEffect(() => {
    if (!load) return;
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [load, reload]);

  const submit = useCallback(async (payload) => {
    const saved = await mentorApi.submitProfile(payload);
    setProfile(saved);
    return saved;
  }, []);

  return { profile, loading, error, reload, submit };
}

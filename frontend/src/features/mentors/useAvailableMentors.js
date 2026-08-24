import { useEffect, useState } from "react";
import { adminApi } from "../../api/adminApi";

/**
 * Which mentors declared one booking's exact hour, for its kind of session.
 *
 * Its own hook so the assign form never names an api/ module - keeping "only
 * hooks import api/" true with no exceptions.
 *
 * `null` while loading is meaningful and not the same as `[]`: an empty array is
 * the real answer "nobody offered this hour", which the form turns into an
 * override prompt. Collapsing the two would flash that warning on every open.
 */
export function useAvailableMentors(requestId) {
  const [mentors, setMentors] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    adminApi
      .availableMentorsFor(requestId)
      .then((rows) => {
        if (!cancelled) setMentors(rows);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [requestId]);

  return { mentors, error, loading: mentors === null && !error };
}

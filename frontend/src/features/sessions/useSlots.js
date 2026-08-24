import { useEffect, useState } from "react";
import { slotApi } from "../../api/slotApi";

/**
 * The bookable-hours grid for one day.
 *
 * These are hours mentors actually declared, not a generated grid - so an empty
 * result is a real answer ("nobody offered that day"), not a bug.
 *
 * `cancelled` matters here more than anywhere else in the app: the date input
 * fires a fetch per keystroke as someone types or arrows through a date, and
 * without it a slow response for the 12th can land after the fast one for the
 * 13th and leave the wrong day's slots on screen. Switching session type
 * re-fetches for the same reason.
 */
export function useSlots(date, sessionType) {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!date) {
      setSlots([]);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    slotApi
      .forDate(date, sessionType)
      .then((list) => {
        if (!cancelled) setSlots(list);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [date, sessionType]);

  return { slots, loading, error };
}

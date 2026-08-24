import { useCallback, useEffect, useState } from "react";
import { adminApi } from "../../api/adminApi";

/**
 * Every mentor's declared hours in a window. The admin's reference view.
 *
 * Grouped by day here rather than in the component, because "what does next week
 * look like" is the question being asked and a flat list does not answer it.
 */
export function useAllAvailability(days = 7) {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setRows(await adminApi.availability(null, days));
  }, [days]);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  // Map keeps insertion order, and the API returns ascending by slot - so days
  // come out chronological without a second sort.
  const byDay = new Map();
  for (const row of rows) {
    const day = row.slotStart.slice(0, 10);
    if (!byDay.has(day)) byDay.set(day, []);
    byDay.get(day).push(row);
  }

  return {
    rows,
    byDay: [...byDay.entries()],
    openCount: rows.filter((r) => r.status === "OPEN").length,
    bookedCount: rows.filter((r) => r.status === "BOOKED").length,
    loading,
    error,
    reload,
  };
}

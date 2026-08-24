import { useCallback, useEffect, useState } from "react";
import { availabilityApi } from "../../api/availabilityApi";

/**
 * A mentor's own declared hours.
 *
 * `lastResult` keeps the server's message from the most recent declaration,
 * because that message is the interesting half of the answer: a mentor who ticked
 * six hours and got four needs to know which two were skipped and why. Dropping
 * it would turn a partial success into a silent one.
 */
export function useAvailability() {
  const [hours, setHours] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastResult, setLastResult] = useState(null);

  const reload = useCallback(async () => {
    setHours(await availabilityApi.mine());
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  const declare = useCallback(async (payload) => {
    setError(null);
    try {
      // requestEnvelope, not request: the server's message names which hours were
      // skipped and why, and that is the half a mentor actually needs.
      const { data, message } = await availabilityApi.declare(payload);
      await reload();
      setLastResult({ count: data.length, message });
      return data;
    } catch (e) {
      setError(e.message);
      throw e;
    }
  }, [reload]);

  const withdraw = useCallback(async (id) => {
    await availabilityApi.withdraw(id);
    await reload();
  }, [reload]);

  return { hours, loading, error, setError, lastResult, reload, declare, withdraw };
}

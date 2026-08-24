import { useCallback, useEffect, useState } from "react";
import { sessionApi } from "../../api/sessionApi";
import { fetchCvBlobUrl } from "../../api/http";
import { partitionSessions, selectUpcoming } from "./sessionRules";

/**
 * A student's bookings: the data, the derived lists, and the actions.
 *
 * Backend counterpart: the facade layer. It composes the api module with the
 * pure rules and hands the screen a finished answer, so the page renders and
 * delegates and holds no fetch logic of its own.
 *
 * Each feature gets its own hook rather than one hook for the whole dashboard.
 * That is what stops a failing plans call from blanking the interview list -
 * they load and fail independently.
 */
export function useSessions() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setSessions(await sessionApi.mine());
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  const book = useCallback(async (form) => {
    const created = await sessionApi.create(form);
    await reload();
    return created;
  }, [reload]);

  const cancel = useCallback(async (id) => {
    await sessionApi.cancel(id);
    await reload();
  }, [reload]);

  const attachCv = useCallback(async (id, file) => {
    await sessionApi.attachCv(id, file);
    await reload();
  }, [reload]);

  const { live, done, unpaidCount } = partitionSessions(sessions);

  return {
    sessions, live, done, unpaidCount,
    upcoming: selectUpcoming(sessions),
    loading, error,
    reload, book, cancel, attachCv,
  };
}

/**
 * Downloading a CV.
 *
 * The endpoint needs the Authorization header, so the bytes cannot go in a plain
 * href - they are fetched as a blob and handed to the browser through a
 * throwaway anchor, which is the only way to give a blob a filename.
 */
export function useCvDownload(session) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const download = useCallback(async () => {
    setBusy(true);
    setError(null);
    let url;
    try {
      url = await fetchCvBlobUrl(session.id);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = session.cvFileName ?? "cv.pdf";
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
    } catch (e) {
      setError(e.message);
    } finally {
      // Safe immediately: click() has already handed the download off.
      if (url) URL.revokeObjectURL(url);
      setBusy(false);
    }
  }, [session]);

  return { download, busy, error };
}

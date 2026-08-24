import { useEffect, useState } from "react";

/**
 * Loads a protected binary (an image, a file) and hands back an object URL.
 *
 * Three cards needed this and each had its own copy of the same twelve lines -
 * including the two things that are easy to get wrong:
 *
 *  - **Revoking on unmount.** An object URL pins the whole blob in memory until
 *    it is revoked. A card that mounts and unmounts as an admin scrolls a queue
 *    would leak one screenshot per open.
 *  - **Ignoring a late arrival.** If the component unmounts while the fetch is
 *    in flight, the response must be revoked rather than stored - otherwise it is
 *    both a leak and a setState on something that is gone.
 *
 * @param fetcher async (id) => objectUrl. One of the api/http.js blob helpers.
 * @param id      what to load. Null/undefined means "don't load yet".
 * @param enabled gate for lazy loading - these are only fetched once a card is
 *                actually expanded, so a queue of thirty does not pull thirty
 *                images nobody asked to see.
 */
export function useBlobUrl(fetcher, id, enabled = true) {
  const [url, setUrl] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!enabled || !id) return undefined;

    let cancelled = false;
    let created = null;

    fetcher(id)
      .then((objectUrl) => {
        created = objectUrl;
        if (cancelled) URL.revokeObjectURL(objectUrl);
        else setUrl(objectUrl);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });

    return () => {
      cancelled = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [fetcher, id, enabled]);

  return { url, error, loading: enabled && !url && !error };
}

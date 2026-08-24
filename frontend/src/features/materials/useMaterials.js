import { useCallback, useEffect, useState } from "react";
import { materialApi } from "../../api/materialApi";
import { fetchMaterialBlobUrl } from "../../api/http";

/**
 * Study material shared with this student.
 *
 * The list is whatever the server says it is - the filtering ("everyone", "just
 * you", "your plan") happens in SQL, and deliberately not here. Anything this
 * hook hid would still be sitting in the response for anyone to read.
 */
export function useMaterials() {
  const [materials, setMaterials] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setMaterials(await materialApi.mine());
  }, []);

  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);

  return { materials, loading, error, reload };
}

/**
 * Downloading one file.
 *
 * The endpoint needs an Authorization header, so the bytes cannot go in a plain
 * href - they are fetched as a blob and handed to the browser through a
 * throwaway anchor, which is the only way to give a blob a filename.
 */
export function useMaterialDownload(material) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const download = useCallback(async () => {
    setBusy(true);
    setError(null);
    let url;
    try {
      url = await fetchMaterialBlobUrl(material.id);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = material.fileName ?? material.title;
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
  }, [material]);

  return { download, busy, error };
}

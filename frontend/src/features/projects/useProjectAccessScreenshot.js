import { fetchProjectScreenshotUrl } from "../../api/http";
import { useBlobUrl } from "../../hooks/useBlobUrl";

/**
 * The payment screenshot for one project access request.
 *
 * Thin, and it exists so the review card never names an api/ module itself -
 * which keeps "only hooks import api/" true with no exceptions.
 */
export function useProjectAccessScreenshot(access, open) {
  return useBlobUrl(fetchProjectScreenshotUrl, access.hasScreenshot ? access.id : null, open);
}

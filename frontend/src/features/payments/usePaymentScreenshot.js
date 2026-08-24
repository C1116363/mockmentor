import { fetchScreenshotUrl } from "../../api/http";
import { useBlobUrl } from "../../hooks/useBlobUrl";

/**
 * The payment screenshot for one interview payment.
 *
 * Thin, and it exists so the review card never names an api/ module itself -
 * which keeps "only hooks import api/" true with no exceptions to remember.
 *
 * @param open only fetch once the card is expanded, so a queue of thirty does
 *             not pull thirty images nobody asked to see.
 */
export function usePaymentScreenshot(payment, open) {
  return useBlobUrl(fetchScreenshotUrl, payment.hasScreenshot ? payment.id : null, open);
}

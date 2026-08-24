import { fetchPlanScreenshotUrl } from "../../api/http";
import { useBlobUrl } from "../../hooks/useBlobUrl";

/** The payment screenshot for one plan purchase. See usePaymentScreenshot. */
export function useEnrollmentScreenshot(enrollment, open) {
  return useBlobUrl(
    fetchPlanScreenshotUrl,
    enrollment.hasScreenshot ? enrollment.id : null,
    open
  );
}

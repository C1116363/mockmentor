import { BASE_URL, request } from "./http";

/** Backend: PaymentController -> /api/payments (paying for one booked session) */
export const paymentApi = {
  instructions: () => request("/payments/instructions"),

  forRequest: (requestId) => request(`/payments/by-request/${requestId}`),

  submitProof: (requestId, upiReference, screenshot) => {
    const body = new FormData();
    body.append("upiReference", upiReference);
    body.append("screenshot", screenshot);
    return request(`/payments/by-request/${requestId}/proof`, { method: "POST", body });
  },

  /**
   * The URL only. The image needs an Authorization header, so it cannot go in an
   * <img src> - fetch it with fetchScreenshotUrl() from http.js instead.
   */
  screenshotUrl: (paymentId) => `${BASE_URL}/payments/${paymentId}/screenshot`,
};

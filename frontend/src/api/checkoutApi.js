import { request } from "./http";

/** Backend: CheckoutController -> /api/checkout (paying through a gateway) */
export const checkoutApi = {
  /**
   * Which payment methods this server can actually offer.
   *
   * Asked at runtime rather than baked in at build time: which gateway is
   * configured is a property on the backend, and its keys can be missing even
   * when one is selected. A hardcoded answer would mean a rebuild to switch
   * payment method, and a Pay-by-card button that exists because somebody meant
   * to configure a gateway rather than because they did.
   */
  options: () => request("/checkout/options"),

  /**
   * Open a gateway order.
   *
   * `purpose` is INTERVIEW, PLAN or PROJECT; `targetId` is the id of the row
   * being paid for. Note there is no amount - the server reads it from that row.
   * If this function took one, anyone could set their own price.
   */
  start: (purpose, targetId) =>
    request(`/checkout/${purpose}/${targetId}`, { method: "POST" }),

  /** Hand back what the checkout window returned, for the server to verify. */
  confirm: (result) =>
    request("/checkout/confirm", {
      method: "POST",
      body: JSON.stringify({
        razorpayOrderId: result.razorpay_order_id,
        razorpayPaymentId: result.razorpay_payment_id,
        razorpaySignature: result.razorpay_signature,
      }),
    }),
};

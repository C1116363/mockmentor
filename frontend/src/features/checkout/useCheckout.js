import { useCallback, useEffect, useState } from "react";
import { checkoutApi } from "../../api/checkoutApi";
import { loadRazorpay } from "./loadRazorpay";

/**
 * Paying through a gateway.
 *
 * One hook for all three things this app sells - an interview, a plan, project
 * access - because from here they are identical: open an order for a purpose
 * and an id, put the checkout in front of the student, hand the signed result
 * back for the server to verify. Only the two arguments differ.
 *
 * @param purpose  "INTERVIEW" | "PLAN" | "PROJECT"
 * @param targetId id of the row being paid for
 */
export function useCheckout(purpose, targetId) {
  const [options, setOptions] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  /**
   * True once the money is taken and the purchase is on.
   *
   * Separate from the parent closing the modal so the student sees a moment of
   * confirmation. A payment screen that vanishes the instant it succeeds leaves
   * people unsure whether it did.
   */
  const [paid, setPaid] = useState(false);

  useEffect(() => {
    let alive = true;
    checkoutApi
      .options()
      .then((data) => alive && setOptions(data))
      // Deliberately silent. This only decides whether an extra button appears;
      // failing to answer it should leave the manual UPI flow untouched, not put
      // an error on a screen where nothing has gone wrong yet.
      .catch(() => alive && setOptions(null))
    return () => {
      alive = false;
    };
  }, []);

  const gatewayAvailable = Boolean(options?.gatewayReady);

  const pay = useCallback(
    async (onPaid) => {
      setBusy(true);
      setError(null);

      try {
        // Loaded before the order is opened. The other order works too, but it
        // leaves an unpaid order at the gateway every time the script fails -
        // which shows up in their dashboard looking like failed payments.
        const Razorpay = await loadRazorpay();
        const checkout = await checkoutApi.start(purpose, targetId);

        await new Promise((resolve, reject) => {
          const rzp = new Razorpay({
            key: checkout.keyId,
            order_id: checkout.orderId,

            // Sent so the window displays the right number, but not trusted:
            // the amount that gets charged is the one on the order, which was
            // set server-side. Editing this in devtools changes the label and
            // nothing else.
            amount: checkout.amountInMinorUnits,
            currency: checkout.currency,

            name: "ConfirmPlacement",
            description: checkout.description,
            prefill: { name: checkout.studentName, email: checkout.studentEmail },
            theme: { color: "#4f46e5" },

            handler: async (result) => {
              try {
                // The signed result goes to the server to be verified. Nothing
                // is marked paid on the strength of this callback alone - this
                // object came from a browser, so only the signature makes it
                // mean anything, and only the server can check that.
                const confirmed = await checkoutApi.confirm(result);
                setPaid(true);
                onPaid?.(confirmed);
                resolve();
              } catch (err) {
                // Money has very likely been taken at this point, so this must
                // never read as "payment failed" - that invites paying twice.
                reject(new Error(
                  err.message
                    + " Your payment may still have gone through — don't pay again. "
                    + "Reload this page in a minute, or contact us with your payment id."
                ));
              }
            },

            modal: {
              // Closing the window is not an error and not a failure. The order
              // stays open, and clicking Pay again returns to the same one.
              ondismiss: () => resolve(),
            },
          });

          // A card decline. The checkout window stays open on its own error
          // screen so the student can try another card, so this only records
          // the reason rather than tearing anything down.
          rzp.on("payment.failed", (event) => {
            setError(event?.error?.description ?? "That payment didn't go through.");
          });

          rzp.open();
        });
      } catch (err) {
        setError(err.message);
      } finally {
        setBusy(false);
      }
    },
    [purpose, targetId]
  );

  return { options, gatewayAvailable, pay, busy, paid, error, setError };
}

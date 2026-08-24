/**
 * The Razorpay checkout script, loaded only when somebody is actually paying.
 *
 * <h2>Why not a <script> tag in index.html</h2>
 * It would run on every page load - the login screen, the mentor dashboard,
 * every screen that has nothing to do with money - to give a third party a
 * script tag on all of them. Loading it at the moment it is needed costs about
 * 200ms once, and that happens while the student is reading the amount.
 */
const SRC = "https://checkout.razorpay.com/v1/checkout.js";

/**
 * Module-level, so the script is fetched once per page load no matter how many
 * components ask for it. Without this cache, opening the pay modal three times
 * appends three <script> tags.
 */
let pending = null;

export function loadRazorpay() {
  if (window.Razorpay) return Promise.resolve(window.Razorpay);
  if (pending) return pending;

  pending = new Promise((resolve, reject) => {
    const tag = document.createElement("script");
    tag.src = SRC;
    tag.async = true;

    tag.onload = () => {
      // onload fires for a 200 that returned something unusable too, so check
      // for what we actually came for rather than trusting the event.
      if (window.Razorpay) resolve(window.Razorpay);
      else reject(new Error("The payment window failed to load. Please refresh and try again."));
    };

    tag.onerror = () => {
      // Cleared so a later attempt retries rather than returning this same
      // rejected promise forever - the usual cause is a flaky network or an ad
      // blocker, and both can be fixed and retried within one page load.
      pending = null;
      reject(new Error(
        "Couldn't reach the payment provider. Check your connection, or pay by UPI instead."
      ));
    };

    document.body.appendChild(tag);
  });

  return pending;
}

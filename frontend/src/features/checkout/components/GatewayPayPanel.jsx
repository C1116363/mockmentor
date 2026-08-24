import { useCheckout } from "../useCheckout";

/**
 * "Pay now" - the gateway half of a payment screen.
 *
 * <h2>Renders nothing when there is no gateway</h2>
 * Every payment screen mounts this unconditionally and it disappears on its own
 * when the server says no gateway is configured. The alternative - each modal
 * asking first and deciding whether to render - is the same question answered
 * in three places, and three places to forget it.
 *
 * <h2>Why manual UPI stays on screen underneath</h2>
 * It costs us nothing where the gateway takes about 2.4%, and it is the only
 * way to take money on a day the gateway is down. So this is presented as the
 * faster option rather than the only one, and the UPI steps below are left
 * exactly as they were.
 */
export default function GatewayPayPanel({ purpose, targetId, amount, onPaid }) {
  const { gatewayAvailable, pay, busy, paid, error } = useCheckout(purpose, targetId);

  if (!gatewayAvailable) return null;

  if (paid) {
    return (
      <div className="gw gw--done">
        <span className="gw__tick" aria-hidden="true">✓</span>
        <div>
          <strong>Payment received</strong>
          <small>You&apos;re all set — nothing else to send us.</small>
        </div>
      </div>
    );
  }

  return (
    <div className="gw">
      <div className="gw__head">
        <div>
          <strong>Pay now</strong>
          <small>Card, netbanking, UPI or a wallet. Confirmed instantly.</small>
        </div>
        <span className="gw__amount">₹{amount}</span>
      </div>

      <button
        type="button"
        className="btn btn--primary btn--wide"
        onClick={() => pay(onPaid)}
        disabled={busy}
      >
        {busy ? (
          <>
            <span className="spinner" /> Opening secure checkout
          </>
        ) : (
          `Pay ₹${amount}`
        )}
      </button>

      {error && <p className="notice notice--error">{error}</p>}

      <p className="gw__note">
        You&apos;ll pay on the provider&apos;s own secure window. We never see your card
        details.
      </p>

      {/* The point of the divider: what follows is an alternative, not the next
          step. Without it the UPI instructions below read as something you still
          have to do after paying. */}
      <div className="gw__or">
        <span>or pay by UPI yourself</span>
      </div>
    </div>
  );
}

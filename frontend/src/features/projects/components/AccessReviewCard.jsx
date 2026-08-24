import { useState } from "react";
import { useProjectAccessScreenshot } from "../useProjectAccessScreenshot";
import { daysLeftLabel } from "../projectRules";
import { formatPrice } from "../../../utils/format";

/**
 * One access request in an admin queue.
 *
 * Renders three different jobs, because the same row means different things at
 * different points and collapsing them would hide two of them:
 *
 *   mode="payment"  check the UTR, then approve or reject
 *   mode="invite"   payment already confirmed — add them on GitHub, then confirm
 *   mode="expired"  access has run out and they are still on the repo — revoke
 */
export default function AccessReviewCard({ access, mode, onApprove, onReject,
                                          onConfirmInvite, onRevoke }) {
  const [open, setOpen] = useState(false);
  const { url: imageUrl, error: imageError } = useProjectAccessScreenshot(access, open);

  const repoSettingsUrl = access.repoFullName
    ? `https://github.com/${access.repoFullName}/settings/access`
    : null;

  return (
    <article className={`card ${mode === "invite" ? "card--action" : ""}`}>
      <header className="card__head">
        <div>
          <h4 className="card__title">
            {access.projectName} · ₹{formatPrice(access.pricePaid)}
          </h4>
          <p className="card__sub">
            {access.studentName} → <span className="mono">@{access.githubUsername}</span>
          </p>
        </div>
        <span className={`badge ${
          mode === "payment" ? "badge--pending"
          : mode === "invite" ? "badge--awaiting_payment"
          : "badge--rejected"}`}>
          {mode === "payment" ? "TO CHECK" : mode === "invite" ? "ADD ON GITHUB" : "EXPIRED"}
        </span>
      </header>

      <dl className="card__facts">
        <div>
          <dt>GitHub account</dt>
          <dd className="mono">@{access.githubUsername}</dd>
        </div>
        <div>
          <dt>Repository</dt>
          <dd className="mono">{access.repoFullName ?? "—"}</dd>
        </div>
        {mode === "payment" && (
          <>
            <div>
              <dt>UTR / reference</dt>
              <dd className="mono">{access.upiReference ?? "—"}</dd>
            </div>
            <div>
              <dt>Sent</dt>
              <dd>{access.submittedAt ? new Date(access.submittedAt).toLocaleString() : "—"}</dd>
            </div>
          </>
        )}
        {mode !== "payment" && (
          <>
            <div>
              <dt>Access window</dt>
              <dd>{daysLeftLabel(access.expiresAt) ?? "—"}</dd>
            </div>
            <div>
              <dt>Student</dt>
              <dd className="muted">{access.studentEmail}</dd>
            </div>
          </>
        )}
      </dl>

      {access.motivation && (
        <p className="card__notes">&ldquo;{access.motivation}&rdquo;</p>
      )}

      {/* The exact next action, with the deep link. An admin should never have to
          go hunting for the settings page themselves. */}
      {mode === "invite" && repoSettingsUrl && (
        <div className="next-step">
          <strong>Add them, then confirm below</strong>
          <p>
            Add <span className="mono">@{access.githubUsername}</span> to{" "}
            <span className="mono">{access.repoFullName}</span> with{" "}
            <strong>Write</strong> access.
          </p>
          <a className="btn btn--ghost btn--sm" href={repoSettingsUrl}
             target="_blank" rel="noopener noreferrer">
            Open repo access settings →
          </a>
        </div>
      )}

      {mode === "expired" && repoSettingsUrl && (
        <div className="next-step next-step--warn">
          <strong>They still have access</strong>
          <p>
            This window closed {daysLeftLabel(access.expiresAt)}. Remove{" "}
            <span className="mono">@{access.githubUsername}</span> from the repo and
            revoke below — our record going stale does not take their push access away.
          </p>
          <a className="btn btn--ghost btn--sm" href={repoSettingsUrl}
             target="_blank" rel="noopener noreferrer">
            Open repo access settings →
          </a>
        </div>
      )}

      {mode === "payment" && (
        access.hasScreenshot ? (
          <>
            <button className="linkish" onClick={() => setOpen((v) => !v)}>
              {open ? "Hide screenshot" : "Show screenshot"}
            </button>
            {open && (
              <>
                {imageError && <p className="notice notice--error">{imageError}</p>}
                {!imageUrl && !imageError && <p className="empty">Loading image...</p>}
                {imageUrl && <img className="pay-shot" src={imageUrl} alt="Payment screenshot" />}
              </>
            )}
          </>
        ) : (
          <p className="empty">No screenshot uploaded.</p>
        )
      )}

      {mode === "payment" && (
        <p className="pay-note">
          Check this UTR against your bank before approving. Approving starts their
          access window — you still have to add them on GitHub afterwards.
        </p>
      )}

      <div className="accept-form__actions">
        {mode === "payment" && (
          <>
            <button className="btn btn--primary" onClick={() => onApprove(access)}>
              Payment received
            </button>
            <button className="btn btn--ghost" onClick={() => onReject(access)}>
              Reject
            </button>
          </>
        )}
        {mode === "invite" && (
          <>
            <button className="btn btn--primary" onClick={() => onConfirmInvite(access)}>
              I&apos;ve added them
            </button>
            <button className="btn btn--ghost" onClick={() => onRevoke(access)}>
              Revoke instead
            </button>
          </>
        )}
        {mode === "expired" && (
          <button className="btn btn--primary" onClick={() => onRevoke(access)}>
            Revoke access
          </button>
        )}
      </div>
    </article>
  );
}

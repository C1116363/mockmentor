import { useCvDownload } from "../useSessions";

const KB = 1024;

const readableSize = (bytes) =>
  !bytes ? null : bytes < KB * KB ? `${Math.round(bytes / KB)} KB` : `${(bytes / KB / KB).toFixed(1)} MB`;

/**
 * The candidate's CV on a booking card.
 *
 * A plain link cannot work here - the endpoint needs the Authorization header -
 * so it fetches the bytes and hands them to the browser through a throwaway
 * anchor. An error shows on the chip rather than in a page-level banner, because
 * the thing that failed is right here.
 */
export default function CvChip({ session }) {
  const { download, busy, error } = useCvDownload(session);

  return (
    <div className="cv-chip">
      <button className="cv-chip__btn" onClick={download} disabled={busy}
              title={`Download ${session.cvFileName ?? "the CV"}`}>
        <span className="cv-chip__icon" aria-hidden="true">📄</span>
        <span className="cv-chip__text">
          <strong>{busy ? "Opening..." : "CV attached"}</strong>
          <small>
            {session.cvFileName}
            {readableSize(session.cvSizeBytes) && ` · ${readableSize(session.cvSizeBytes)}`}
          </small>
        </span>
        <span className="cv-chip__go" aria-hidden="true">↓</span>
      </button>
      {error && <small className="field__error">{error}</small>}
    </div>
  );
}

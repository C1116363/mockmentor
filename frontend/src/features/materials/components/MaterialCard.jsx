import { useMaterialDownload } from "../useMaterials";

const KB = 1024;

function readableSize(bytes) {
  if (!bytes) return null;
  if (bytes < KB) return `${bytes} B`;
  if (bytes < KB * KB) return `${Math.round(bytes / KB)} KB`;
  return `${(bytes / (KB * KB)).toFixed(1)} MB`;
}

/** A rough icon per file type. Cosmetic only - the backend decides what is allowed. */
function iconFor(material) {
  if (material.kind === "LINK") return "🔗";
  const type = material.contentType ?? "";
  if (type.includes("pdf")) return "📕";
  if (type.startsWith("image/")) return "🖼";
  if (type.includes("zip")) return "🗜";
  if (type.includes("word") || type.includes("msword")) return "📝";
  if (type.includes("presentation") || type.includes("powerpoint")) return "📊";
  if (type.includes("sheet") || type.includes("excel")) return "📈";
  return "📄";
}

/**
 * One piece of study material, as a student sees it.
 *
 * A file cannot go in a plain <a href> - the download endpoint needs the
 * Authorization header - so it is fetched as a blob and saved through a
 * temporary object URL. A link is just a link.
 */
export default function MaterialCard({ material }) {
  const { download, busy, error } = useMaterialDownload(material);

  return (
    <article className="material">
      <span className="material__icon" aria-hidden="true">{iconFor(material)}</span>

      <div className="material__body">
        <h4 className="material__title">{material.title}</h4>
        {material.description && <p className="material__desc">{material.description}</p>}

        <p className="material__meta">
          <span className="material__chip">{material.audienceLabel}</span>
          {material.fileName && <span className="mono">{material.fileName}</span>}
          {readableSize(material.sizeBytes) && <span>{readableSize(material.sizeBytes)}</span>}
          <span>{new Date(material.createdAt).toLocaleDateString()}</span>
        </p>

        {error && <p className="notice notice--error">{error}</p>}
      </div>

      <div className="material__action">
        {material.kind === "LINK" ? (
          <a
            className="btn btn--ghost btn--sm"
            href={material.linkUrl}
            target="_blank"
            // noreferrer as well as noopener: without it the opened page can
            // read where it was linked from, and window.opener stays reachable
            // in older browsers.
            rel="noopener noreferrer"
          >
            Open link
          </a>
        ) : (
          <button className="btn btn--ghost btn--sm" onClick={download} disabled={busy}>
            {busy ? "..." : "Download"}
          </button>
        )}
      </div>
    </article>
  );
}

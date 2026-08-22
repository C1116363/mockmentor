import { useState } from "react";

const WORDS = ["", "Poor", "Below par", "Solid", "Strong", "Excellent"];

/**
 * Clickable 1-5 stars.
 *
 * Hovering previews a score without committing it, which is what makes rating
 * feel responsive rather than like a dropdown.
 *
 * Rendered as real radio inputs underneath, so it works with a keyboard and
 * screen readers instead of being a pile of unlabelled divs.
 */
export default function StarRating({ name, value, onChange, label, hint }) {
  const [hover, setHover] = useState(0);
  const shown = hover || value || 0;

  return (
    <div className="rating">
      <div className="rating__top">
        <span className="rating__label">
          {label}
          {hint && <small>{hint}</small>}
        </span>
        <span className={`rating__word ${shown ? "" : "rating__word--empty"}`}>
          {shown ? `${shown}/5 · ${WORDS[shown]}` : "Not rated"}
        </span>
      </div>

      <div className="rating__stars" onMouseLeave={() => setHover(0)}>
        {[1, 2, 3, 4, 5].map((n) => (
          <label
            key={n}
            className={`star ${n <= shown ? "star--on" : ""}`}
            onMouseEnter={() => setHover(n)}
            title={`${n} — ${WORDS[n]}`}
          >
            <input
              type="radio"
              name={name}
              value={n}
              checked={value === n}
              onChange={() => onChange(n)}
            />
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 2.5l2.9 6.1 6.6.9-4.8 4.6 1.2 6.6L12 17.6 6.1 20.7l1.2-6.6L2.5 9.5l6.6-.9L12 2.5z" />
            </svg>
            <span className="sr-only">
              {n} out of 5 for {label}
            </span>
          </label>
        ))}
      </div>
    </div>
  );
}

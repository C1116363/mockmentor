import { useEffect, useRef, useState } from "react";

/**
 * How long a plain confirmation stays before fading out.
 *
 * Four seconds, not two. Two is enough to notice a message and not enough to
 * read one - "Payout raised for Ananya Rao: 2 interviews + 2 mentoring
 * sessions = ₹2,600" is a real message from this app and takes longer than
 * that. One number, change it here.
 */
const DISMISS_MS = 4000;

/** Anything that looks like a link the reader is meant to follow. */
const URL_PATTERN = /(https?:\/\/[^\s]+)/g;

/**
 * A message that gets out of the way on its own - unless it shouldn't.
 *
 * <h2>Why not just hide everything after a timer</h2>
 * Some of these messages are instructions. Approving project access returns
 *
 *   "Payment confirmed. Add @someone to org/repo as a collaborator:
 *    https://github.com/org/repo/settings/access"
 *
 * which is the admin's actual next step, with the URL they need. Fading that
 * out after a couple of seconds would leave a paying student without repo
 * access and nothing on screen to say so. So the rule is about what the message
 * is for, not how old it is:
 *
 * <ul>
 *   <li><b>Plain confirmations</b> fade after {@code DISMISS_MS}. Nothing is
 *       lost - the thing they describe already happened and is on screen.</li>
 *   <li><b>Errors</b> stay. You have to read what went wrong, and they are
 *       usually the longest messages here.</li>
 *   <li><b>Anything containing a link</b> stays, because a link is something
 *       the reader is meant to act on.</li>
 * </ul>
 *
 * Everything gets a dismiss button either way, so a message that stays is never
 * stuck.
 */
export default function Notice({ tone = "success", children, onDismiss, sticky = false }) {
  const [leaving, setLeaving] = useState(false);
  const timer = useRef(null);
  // Whether the pointer is over the message. A timer that keeps running while
  // somebody is reading is how a notice disappears mid-sentence.
  const [held, setHeld] = useState(false);

  const text = typeof children === "string" ? children : "";
  const hasLink = URL_PATTERN.test(text);
  // .test() advances lastIndex on a /g regex, so the next call on the same
  // pattern starts from the wrong place and returns a wrong answer. Resetting
  // is not optional here - this exact bug makes every second message behave
  // differently from the first.
  URL_PATTERN.lastIndex = 0;

  const autoDismiss = !sticky && tone !== "error" && !hasLink && Boolean(onDismiss);

  useEffect(() => {
    if (!autoDismiss || held) return undefined;

    timer.current = setTimeout(() => setLeaving(true), DISMISS_MS);
    return () => clearTimeout(timer.current);
  }, [autoDismiss, held]);

  // Removed after the fade rather than at the same moment, so it slides away
  // instead of blinking out.
  useEffect(() => {
    if (!leaving) return undefined;
    const t = setTimeout(() => onDismiss?.(), 260);
    return () => clearTimeout(t);
  }, [leaving, onDismiss]);

  return (
    <p
      className={`notice notice--${tone} ${leaving ? "notice--leaving" : ""}`}
      role={tone === "error" ? "alert" : "status"}
      onMouseEnter={() => setHeld(true)}
      onMouseLeave={() => setHeld(false)}
    >
      <span className="notice__text">{linkify(text || children)}</span>

      {onDismiss && (
        <button
          type="button"
          className="notice__x"
          onClick={() => onDismiss()}
          aria-label="Dismiss"
        >
          ×
        </button>
      )}
    </p>
  );
}

/**
 * Turn bare URLs in the text into real links.
 *
 * The server sends these as plain text, so until now "go to this URL" meant
 * selecting it and copying it by hand. Built by splitting into React nodes
 * rather than with dangerouslySetInnerHTML - some of these strings contain
 * user-supplied values like a GitHub username, and injecting them as HTML would
 * turn a status message into stored XSS.
 */
function linkify(value) {
  if (typeof value !== "string") return value;

  const parts = value.split(URL_PATTERN);
  URL_PATTERN.lastIndex = 0;

  return parts.map((part, i) =>
    /^https?:\/\//.test(part) ? (
      <a
        key={i}
        href={part}
        target="_blank"
        // noopener stops the opened page reaching back through window.opener;
        // noreferrer keeps our URL out of its referrer header.
        rel="noopener noreferrer"
        className="notice__link"
      >
        {part}
      </a>
    ) : (
      part
    )
  );
}

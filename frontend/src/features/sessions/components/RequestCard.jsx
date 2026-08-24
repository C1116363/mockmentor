import StatusBadge from "../../../components/StatusBadge";
import FeedbackCard from "./FeedbackCard";

function formatDateTime(value) {
  if (!value) return null;
  return new Date(value).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

/** "20 Sep 2026, 3:00–4:00 PM" */
function formatSlot(start, end) {
  if (!start) return null;
  const s = new Date(start);
  const day = s.toLocaleDateString(undefined, { dateStyle: "medium" });
  const from = s.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
  if (!end) return `${day}, ${from}`;
  const to = new Date(end).toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
  return `${day}, ${from} – ${to}`;
}

/**
 * One booking. `children` lets each dashboard slot in its own actions.
 *
 * A mentoring session is not an interview, so the wording follows the type - an
 * "Interviewer" row and a "Join the interview" button on a booking somebody made
 * specifically to *avoid* being interviewed reads as a bug.
 *
 * `sessionLabel` and `scored` come from the API rather than being mapped from
 * the enum here, so the words cannot drift between this card and the server.
 */
export default function RequestCard({ request, children }) {
  const mentoring = request.sessionType === "MENTORING";

  return (
    <article className="card">
      <header className="card__head">
        <div>
          <h4 className="card__title">{request.topic}</h4>
          <p className="card__sub">
            <span className={`kind ${mentoring ? "kind--mentoring" : "kind--interview"}`}>
              {mentoring ? "💬" : "🎙️"} {request.sessionLabel ?? "Mock interview"}
            </span>
            <span className="card__sub-sep">·</span>
            {request.experienceLevel}
          </p>
        </div>
        <StatusBadge status={request.status} />
      </header>

      <dl className="card__facts">
        <div>
          <dt>Requested slot</dt>
          <dd>{formatSlot(request.preferredSlot, request.preferredSlotEnd)}</dd>
        </div>
        {request.mentor && (
          <div>
            <dt>{mentoring ? "Mentor" : "Interviewer"}</dt>
            <dd>{request.mentor.name}</dd>
          </div>
        )}
        {request.scheduledAt && (
          <div>
            <dt>Scheduled for</dt>
            <dd>{formatDateTime(request.scheduledAt)}</dd>
          </div>
        )}
      </dl>

      {request.notes && <p className="card__notes">&ldquo;{request.notes}&rdquo;</p>}

      {request.meetingLink && (
        <a className="card__link" href={request.meetingLink} target="_blank" rel="noreferrer">
          {mentoring ? "Join the session" : "Join the interview"} &rarr;
        </a>
      )}

      <FeedbackCard request={request} />

      {children}
    </article>
  );
}

import StatusBadge from "./StatusBadge";

function formatDateTime(value) {
  if (!value) return null;
  return new Date(value).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

function formatDate(value) {
  if (!value) return null;
  return new Date(value).toLocaleDateString(undefined, { dateStyle: "medium" });
}

/**
 * One interview request. `children` lets each dashboard slot in its own actions.
 */
export default function RequestCard({ request, children }) {
  return (
    <article className="card">
      <header className="card__head">
        <div>
          <h4 className="card__title">{request.topic}</h4>
          <p className="card__sub">
            {request.student.fullName} &middot; {request.experienceLevel}
          </p>
        </div>
        <StatusBadge status={request.status} />
      </header>

      <dl className="card__facts">
        <div>
          <dt>Preferred date</dt>
          <dd>{formatDate(request.preferredDate)}</dd>
        </div>
        <div>
          <dt>Student email</dt>
          <dd>{request.student.email}</dd>
        </div>
        {request.mentor && (
          <div>
            <dt>Mentor</dt>
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
          Join the interview &rarr;
        </a>
      )}

      {request.feedback && (
        <div className="card__feedback">
          <strong>Mentor feedback</strong>
          <p>{request.feedback}</p>
        </div>
      )}

      {children}
    </article>
  );
}

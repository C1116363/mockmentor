const VERDICT = {
  READY: { label: "Ready", blurb: "Would pass a real round today", cls: "ready" },
  ALMOST_READY: { label: "Almost there", blurb: "A few gaps to close first", cls: "almost" },
  NEEDS_WORK: { label: "Needs work", blurb: "More preparation needed", cls: "needs" },
};

function Stars({ score }) {
  if (!score) return <span className="score__na">not rated</span>;
  return (
    <span className="score__stars" aria-label={`${score} out of 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} className={n <= score ? "on" : ""}>
          ★
        </span>
      ))}
    </span>
  );
}

/**
 * The scorecard as the candidate sees it.
 *
 * Older interviews were completed before scoring existed, so every field here
 * has to survive being null - hence the checks rather than assuming a rating.
 */
export default function FeedbackCard({ request }) {
  const verdict = VERDICT[request.recommendation];
  const hasScores =
    request.overallRating ||
    request.technicalRating ||
    request.communicationRating ||
    request.problemSolvingRating;

  if (!request.feedback && !hasScores) return null;

  // A mentoring session has no ratings by design - the server nulls them even
  // if a client sends them - so it gets notes with a heading that matches.
  const scored = request.scored !== false;

  return (
    <div className="scorecard">
      <header className="scorecard__head">
        <strong>{scored ? "Your scorecard" : "Notes from your session"}</strong>
        {verdict && (
          <span className={`verdict-pill verdict-pill--${verdict.cls}`} title={verdict.blurb}>
            {verdict.label}
          </span>
        )}
      </header>

      {request.overallRating && (
        <div className="scorecard__overall">
          <span className="scorecard__big">{request.overallRating}</span>
          <span className="scorecard__outof">/ 5 overall</span>
          <Stars score={request.overallRating} />
        </div>
      )}

      {hasScores && (
        <dl className="scores">
          <div>
            <dt>Technical</dt>
            <dd><Stars score={request.technicalRating} /></dd>
          </div>
          <div>
            <dt>Problem solving</dt>
            <dd><Stars score={request.problemSolvingRating} /></dd>
          </div>
          <div>
            <dt>Communication</dt>
            <dd><Stars score={request.communicationRating} /></dd>
          </div>
        </dl>
      )}

      {request.feedback && (
        <div className="scorecard__block">
          <h5>Summary</h5>
          <p>{request.feedback}</p>
        </div>
      )}

      {request.strengths && (
        <div className="scorecard__block scorecard__block--good">
          <h5>What went well</h5>
          <p>{request.strengths}</p>
        </div>
      )}

      {request.improvements && (
        <div className="scorecard__block scorecard__block--work">
          <h5>What to work on</h5>
          <p>{request.improvements}</p>
        </div>
      )}
    </div>
  );
}

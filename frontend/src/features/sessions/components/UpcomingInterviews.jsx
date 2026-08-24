/**
 * The "you have an interview coming up" banner, shared by the student and the
 * mentor dashboard so both sides see the same thing.
 *
 * The Join button opens the room that was created when the admin made the
 * match, so both people land in the same call.
 */
function formatWhen(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString(undefined, {
    weekday: "short",
    day: "numeric",
    month: "short",
    hour: "numeric",
    minute: "2-digit",
  });
}

/** "in 3 days" / "in 2 hours" / "starting now" */
function countdown(value) {
  const ms = new Date(value) - new Date();
  if (ms <= 0) return "starting now";
  const mins = Math.round(ms / 60000);
  if (mins < 60) return `in ${mins} min`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `in ${hours} hour${hours === 1 ? "" : "s"}`;
  const days = Math.round(hours / 24);
  return `in ${days} day${days === 1 ? "" : "s"}`;
}

/**
 * A call is joinable from 15 minutes before the slot until an hour after it
 * ends - clicking "Join" three days early would just drop you into an empty room.
 */
function isJoinable(scheduledAt) {
  const start = new Date(scheduledAt).getTime();
  const now = Date.now();
  return now >= start - 15 * 60 * 1000 && now <= start + 2 * 60 * 60 * 1000;
}

export default function UpcomingInterviews({ interviews, otherPartyLabel, otherParty }) {
  if (interviews.length === 0) return null;

  return (
    <section className="upcoming">
      <header className="upcoming__head">
        <h3>
          Upcoming interview{interviews.length > 1 ? "s" : ""}
          <span className="count">{interviews.length}</span>
        </h3>
      </header>

      <div className="upcoming__list">
        {interviews.map((r) => {
          const joinable = isJoinable(r.scheduledAt);
          return (
            <article className="upcoming__item" key={r.id}>
              <div className="upcoming__info">
                <h4>{r.topic}</h4>
                <p className="upcoming__meta">
                  {otherPartyLabel}: <strong>{otherParty(r)}</strong>
                </p>
                <p className="upcoming__when">
                  {formatWhen(r.scheduledAt)}
                  <span className="upcoming__countdown">{countdown(r.scheduledAt)}</span>
                </p>
              </div>

              {r.meetingLink ? (
                <a
                  className={`btn btn--join ${joinable ? "" : "btn--join-early"}`}
                  href={r.meetingLink}
                  target="_blank"
                  rel="noreferrer"
                  title={joinable ? "Opens the meeting room" : "You can join 15 minutes before it starts"}
                >
                  {joinable ? "Join now" : "Join"}
                </a>
              ) : (
                <span className="upcoming__nolink">No link yet</span>
              )}
            </article>
          );
        })}
      </div>
    </section>
  );
}

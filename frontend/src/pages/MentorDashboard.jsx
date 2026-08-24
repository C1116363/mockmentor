import { useEffect, useState } from "react";
import Notice from "../components/Notice";
import RequestCard from "../features/sessions/components/RequestCard";
import UpcomingInterviews from "../features/sessions/components/UpcomingInterviews";
import FeedbackModal from "../features/sessions/components/FeedbackModal";
import { useSectionNav } from "../layout/SectionNav";
import { useMentorSessions } from "../features/sessions/useMentorSessions";
import { useAvailability } from "../features/mentors/useAvailability";
import AvailabilityPlanner from "../features/mentors/components/AvailabilityPlanner";
import Skeleton from "../components/Skeleton";
import { selectUpcoming } from "../features/sessions/sessionRules";

function defaultSlotFrom(preferred) {
  return preferred ? preferred.slice(0, 16) : "";
}

export default function MentorDashboard() {
  const { active: tab, register, go: setTab } = useSectionNav();

  // The facade layer for this screen. Queue and accepted list reload together,
  // because accepting moves a booking from one to the other.
  const { queue: pending, assigned, loading, reload,
          accept: acceptSession } = useMentorSessions();

  // Its own hook: availability is a separate feature, and a failure loading it
  // should not blank out the interview queue.
  const availability = useAvailability();

  const [message, setMessage] = useState(null);
  const [openId, setOpenId] = useState(null);
  const [slot, setSlot] = useState("");
  const [meetingLink, setMeetingLink] = useState("");
  const [reviewing, setReviewing] = useState(null);

  async function accept(id) {
    setMessage(null);
    try {
      await acceptSession(id, {
        scheduledAt: `${slot}:00`,
        meetingLink: meetingLink.trim() || null,
      });
      setOpenId(null);
      setMeetingLink("");
      setMessage({ type: "success", text: "Scheduled. The candidate can see it now." });
      setTab("mine");
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function afterFeedback() {
    setReviewing(null);
    setMessage({ type: "success", text: "Feedback sent. The candidate can read it now." });
    await reload();
  }

  const upcoming = selectUpcoming(assigned);
  // Zero open hours is worth flagging in the nav: it means this mentor is
  // invisible to every student, which is easy to not notice.
  const openHours = availability.hours.filter((a) => a.status === "OPEN").length;
  const scheduled = assigned.filter((r) => r.status === "SCHEDULED");
  const past = assigned.filter((r) => r.status === "COMPLETED" || r.status === "CANCELLED");

  useEffect(() => {
    register(
      [
        { key: "queue", label: "Open queue", icon: "📥", count: pending.length, alert: pending.length > 0 },
        { key: "mine", label: "My interviews", icon: "📅", count: scheduled.length },
        { key: "history", label: "History", icon: "🗂", count: past.length },
        { key: "availability", label: "My availability", icon: "🗓",
          count: openHours, alert: openHours === 0 },
      ],
      "queue"
    );
  }, [register, pending.length, scheduled.length, past.length, openHours]);

  return (
    <>
      {reviewing && (
        <FeedbackModal
          request={reviewing}
          onDone={afterFeedback}
          onClose={() => setReviewing(null)}
        />
      )}

      <UpcomingInterviews
        interviews={upcoming}
        otherPartyLabel="Candidate"
        otherParty={(r) => r.student?.fullName ?? "—"}
      />

      {message && (
        <Notice tone={message.type} onDismiss={() => setMessage(null)}>
          {message.text}
        </Notice>
      )}

      {tab === "queue" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">Waiting for an interviewer</span>
            <h2>
              Open queue <span className="count">{pending.length}</span>
            </h2>
            <p>
              Mock interviews and mentoring discussions, mixed together — each card says
              which. Pick one, confirm the slot, and share your meeting link, or leave it
              blank and we&apos;ll create a room.
            </p>
          </header>

          {loading && <Skeleton rows={2} />}
          {!loading && pending.length === 0 && (
            <p className="empty">Nothing in the queue right now. Check back later.</p>
          )}

          <div className="card-list">
            {pending.map((r) => (
              <RequestCard key={r.id} request={r}>
                {openId === r.id ? (
                  <div className="accept-form">
                    <label className="field">
                      <span>Confirm the slot</span>
                      <input
                        type="datetime-local"
                        value={slot}
                        onChange={(e) => setSlot(e.target.value)}
                      />
                    </label>
                    <label className="field">
                      <span>Meeting link (optional)</span>
                      <input
                        value={meetingLink}
                        onChange={(e) => setMeetingLink(e.target.value)}
                        placeholder="Leave blank — a room is created for you"
                      />
                    </label>
                    <div className="accept-form__actions">
                      <button className="btn btn--primary" onClick={() => accept(r.id)} disabled={!slot}>
                        Confirm
                      </button>
                      <button className="btn btn--ghost" onClick={() => setOpenId(null)}>
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    className="btn btn--primary"
                    onClick={() => {
                      setOpenId(r.id);
                      setSlot(defaultSlotFrom(r.preferredSlot));
                    }}
                  >
                    {/* RequestCard already shows the kind as a chip, but the
                        button is what gets clicked - a mentor accepting what
                        they think is an interview and finding a discussion is
                        the mistake worth spending three words to prevent. */}
                    {r.sessionType === "MENTORING"
                      ? "Accept this session"
                      : "Accept this interview"}
                  </button>
                )}
              </RequestCard>
            ))}
          </div>
        </section>
      )}

      {tab === "mine" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">Your schedule</span>
            <h2>
              My interviews <span className="count">{scheduled.length}</span>
            </h2>
            <p>
              Accepted and coming up. Write it up once you&apos;ve taken the call — a
              scorecard for an interview, notes for a mentoring session.
            </p>
          </header>

          {scheduled.length === 0 && (
            <div className="empty">
              <p>You haven&apos;t accepted anything yet.</p>
              <button className="btn btn--primary" onClick={() => setTab("queue")}>
                See the open queue
              </button>
            </div>
          )}

          <div className="card-list">
            {scheduled.map((r) => (
              <RequestCard key={r.id} request={r}>
                <button className="btn btn--primary" onClick={() => setReviewing(r)}>
                  Give feedback &amp; close
                </button>
              </RequestCard>
            ))}
          </div>
        </section>
      )}

      {tab === "availability" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">When you&apos;re free</span>
            <h2>
              My availability <span className="count">{openHours}</span>
            </h2>
            <p>
              <strong>This is what students see.</strong> The booking grid is built
              from the hours mentors offer — nothing is generated — so an hour you
              don&apos;t offer is an hour nobody can book. Sessions need a
              day&apos;s notice, so add hours a little ahead.
            </p>
          </header>

          {openHours === 0 && (
            <p className="notice notice--error">
              You have no open hours, so no student can book you. Add some below.
            </p>
          )}

          <AvailabilityPlanner
            onDeclare={availability.declare}
            existing={availability.hours}
          />

          <h3 className="subhead">
            Offered <span className="count">{availability.hours.length}</span>
          </h3>

          {availability.loading && <Skeleton rows={2} lines={2} />}
          {availability.error && (
            <p className="notice notice--error">{availability.error}</p>
          )}
          {!availability.loading && availability.hours.length === 0 && (
            <p className="empty">Nothing offered yet.</p>
          )}

          {availability.hours.length > 0 && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>When</th><th>Taking</th><th>Status</th><th>Booked by</th><th />
                  </tr>
                </thead>
                <tbody>
                  {availability.hours.map((a) => (
                    <tr key={a.id}>
                      <td>
                        {new Date(a.slotStart).toLocaleDateString(undefined, { dateStyle: "medium" })}
                        <div className="muted small">{a.label}</div>
                      </td>
                      <td className="muted small">
                        {a.forInterviews && a.forMentoring
                          ? "Interviews + mentoring"
                          : a.forInterviews ? "Interviews only" : "Mentoring only"}
                      </td>
                      <td>
                        <span className={`badge ${
                          a.status === "BOOKED" ? "badge--scheduled"
                          : a.status === "OPEN" ? "badge--completed"
                          : "badge--cancelled"}`}>
                          {a.status}
                        </span>
                      </td>
                      <td className="muted small">
                        {a.bookedFor
                          ? <>{a.bookedFor}<div className="muted">{a.bookedTopic}</div></>
                          : "—"}
                      </td>
                      <td>
                        {a.status === "OPEN" && (
                          <button className="btn btn--ghost btn--sm"
                                  onClick={async () => {
                                    try {
                                      await availability.withdraw(a.id);
                                      setMessage({ type: "success",
                                        text: `${a.label} withdrawn.` });
                                    } catch (e) {
                                      setMessage({ type: "error", text: e.message });
                                    }
                                  }}>
                            Withdraw
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {tab === "history" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">Past</span>
            <h2>
              History <span className="count">{past.length}</span>
            </h2>
            <p>Sessions you&apos;ve completed, with what you wrote up.</p>
          </header>

          {past.length === 0 && <p className="empty">Nothing completed yet.</p>}

          <div className="card-list">
            {past.map((r) => (
              <RequestCard key={r.id} request={r} />
            ))}
          </div>
        </section>
      )}
    </>
  );
}

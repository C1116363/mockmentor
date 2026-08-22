import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import RequestCard from "../components/RequestCard";
import UpcomingInterviews, { selectUpcoming } from "../components/UpcomingInterviews";
import FeedbackModal from "../components/FeedbackModal";
import { useSectionNav } from "../nav/SectionNav";

function defaultSlotFrom(preferred) {
  return preferred ? preferred.slice(0, 16) : "";
}

export default function MentorDashboard() {
  const { active: tab, register, go: setTab } = useSectionNav();

  const [pending, setPending] = useState([]);
  const [assigned, setAssigned] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const [openId, setOpenId] = useState(null);
  const [slot, setSlot] = useState("");
  const [meetingLink, setMeetingLink] = useState("");
  const [reviewing, setReviewing] = useState(null);

  const reload = useCallback(async () => {
    const [p, a] = await Promise.all([api.pendingRequests(), api.assignedRequests()]);
    setPending(p);
    setAssigned(a);
  }, []);

  useEffect(() => {
    reload()
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, [reload]);

  async function accept(id) {
    setMessage(null);
    try {
      await api.acceptRequest(id, {
        scheduledAt: `${slot}:00`,
        meetingLink: meetingLink.trim() || null,
      });
      setOpenId(null);
      setMeetingLink("");
      setMessage({ type: "success", text: "Scheduled. The candidate can see it now." });
      await reload();
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
  const scheduled = assigned.filter((r) => r.status === "SCHEDULED");
  const past = assigned.filter((r) => r.status === "COMPLETED" || r.status === "CANCELLED");

  useEffect(() => {
    register(
      [
        { key: "queue", label: "Open queue", icon: "📥", count: pending.length, alert: pending.length > 0 },
        { key: "mine", label: "My interviews", icon: "📅", count: scheduled.length },
        { key: "history", label: "History", icon: "🗂", count: past.length },
      ],
      "queue"
    );
  }, [register, pending.length, scheduled.length, past.length]);

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

      {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}

      {tab === "queue" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">Waiting for an interviewer</span>
            <h2>
              Open queue <span className="count">{pending.length}</span>
            </h2>
            <p>Pick one, confirm the slot, and share your meeting link — or leave it blank and we&apos;ll create a room.</p>
          </header>

          {loading && <p className="empty">Loading the queue...</p>}
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
                    Accept this interview
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
            <p>Accepted and coming up. Give feedback once you&apos;ve taken the call.</p>
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

      {tab === "history" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">Past</span>
            <h2>
              History <span className="count">{past.length}</span>
            </h2>
            <p>Interviews you&apos;ve completed, with the feedback you gave.</p>
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

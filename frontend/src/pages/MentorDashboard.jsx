import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import RequestCard from "../components/RequestCard";
import UpcomingInterviews, { selectUpcoming } from "../components/UpcomingInterviews";
import FeedbackModal from "../components/FeedbackModal";

function defaultSlotFrom(preferred) {
  // Default the scheduling input to whatever the candidate asked for.
  if (!preferred) return "";
  return preferred.slice(0, 16); // "2026-09-20T15:00:00" -> "2026-09-20T15:00"
}

/** What an APPROVED mentor sees: the open queue and their own interviews. */
export default function MentorDashboard() {
  const [pending, setPending] = useState([]);
  const [assigned, setAssigned] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const [openId, setOpenId] = useState(null);
  const [slot, setSlot] = useState("");
  const [meetingLink, setMeetingLink] = useState("");
  // Which interview the feedback form is open for.
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

    <div className="sections">
      <section className="panel panel--student">
        <header className="panel__head">
          <span className="panel__tag">Open queue</span>
          <h2>
            Waiting for an interviewer <span className="count">{pending.length}</span>
          </h2>
          <p>Pick one, confirm the slot, and share your meeting link.</p>
        </header>

        {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}
        {loading && <p className="empty">Loading the queue...</p>}
        {!loading && pending.length === 0 && <p className="empty">Nothing in the queue right now.</p>}

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
                    <button
                      className="btn btn--primary"
                      onClick={() => accept(r.id)}
                      disabled={!slot}
                    >
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

      <section className="panel panel--mentor">
        <header className="panel__head">
          <span className="panel__tag">Your schedule</span>
          <h2>
            My interviews <span className="count">{assigned.length}</span>
          </h2>
        </header>

        {assigned.length === 0 && <p className="empty">You haven&apos;t accepted anything yet.</p>}

        <div className="card-list">
          {assigned.map((r) => (
            <RequestCard key={r.id} request={r}>
              {r.status === "SCHEDULED" && (
                <button className="btn btn--primary" onClick={() => setReviewing(r)}>
                  Give feedback &amp; close
                </button>
              )}
            </RequestCard>
          ))}
        </div>
      </section>
    </div>
    </>
  );
}

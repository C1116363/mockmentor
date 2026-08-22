import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import RequestCard from "../components/RequestCard";

/** Default the picker to "tomorrow 10:00". */
function defaultSlot() {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(10, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, "0");
  return `${tomorrow.getFullYear()}-${pad(tomorrow.getMonth() + 1)}-${pad(tomorrow.getDate())}T${pad(
    tomorrow.getHours()
  )}:${pad(tomorrow.getMinutes())}`;
}

/**
 * The mentor's view. The "who am I signing in as" dropdown is gone - that was a
 * placeholder for real authentication, and now we have it.
 */
export default function MentorDashboard() {
  const [pending, setPending] = useState([]);
  const [assigned, setAssigned] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const [openRequestId, setOpenRequestId] = useState(null);
  const [slot, setSlot] = useState(defaultSlot);
  const [meetingLink, setMeetingLink] = useState("");

  const reload = useCallback(async () => {
    const [pendingList, assignedList] = await Promise.all([
      api.pendingRequests(),
      api.assignedRequests(),
    ]);
    setPending(pendingList);
    setAssigned(assignedList);
  }, []);

  useEffect(() => {
    reload()
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, [reload]);

  async function acceptRequest(requestId) {
    setMessage(null);
    try {
      // No mentorId in the body: the server reads it from the token.
      await api.acceptRequest(requestId, { scheduledAt: slot, meetingLink });
      setOpenRequestId(null);
      setMeetingLink("");
      setSlot(defaultSlot());
      setMessage({ type: "success", text: "Interview scheduled. The student can see it now." });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function completeRequest(requestId) {
    const feedback = window.prompt("How did the interview go? (saved as feedback)");
    if (!feedback) return;

    try {
      await api.completeRequest(requestId, { feedback });
      setMessage({ type: "success", text: "Marked as completed." });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  return (
    <div className="sections">
      <section className="panel panel--student">
        <header className="panel__head">
          <span className="panel__tag">Open queue</span>
          <h2>
            Requests waiting <span className="count">{pending.length}</span>
          </h2>
          <p>Pick one, choose a slot, and share your meeting link.</p>
        </header>

        {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}
        {loading && <p className="empty">Loading the queue...</p>}
        {!loading && pending.length === 0 && <p className="empty">Nothing in the queue right now.</p>}

        <div className="card-list">
          {pending.map((request) => (
            <RequestCard key={request.id} request={request}>
              {openRequestId === request.id ? (
                <div className="accept-form">
                  <label className="field">
                    <span>Slot</span>
                    <input
                      type="datetime-local"
                      value={slot}
                      onChange={(event) => setSlot(event.target.value)}
                    />
                  </label>
                  <label className="field">
                    <span>Meeting link</span>
                    <input
                      value={meetingLink}
                      onChange={(event) => setMeetingLink(event.target.value)}
                      placeholder="https://meet.google.com/abc-defg-hij"
                    />
                  </label>
                  <div className="accept-form__actions">
                    <button
                      className="btn btn--primary"
                      onClick={() => acceptRequest(request.id)}
                      disabled={!meetingLink.trim()}
                    >
                      Confirm
                    </button>
                    <button className="btn btn--ghost" onClick={() => setOpenRequestId(null)}>
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <button className="btn btn--primary" onClick={() => setOpenRequestId(request.id)}>
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
          <p>Everything you have accepted.</p>
        </header>

        {assigned.length === 0 && <p className="empty">You haven&apos;t accepted anything yet.</p>}

        <div className="card-list">
          {assigned.map((request) => (
            <RequestCard key={request.id} request={request}>
              {request.status === "SCHEDULED" && (
                <button className="btn btn--ghost" onClick={() => completeRequest(request.id)}>
                  Mark completed &amp; add feedback
                </button>
              )}
            </RequestCard>
          ))}
        </div>
      </section>
    </div>
  );
}

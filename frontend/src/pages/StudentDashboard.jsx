import { useEffect, useState } from "react";
import { api } from "../api/client";
import RequestCard from "../components/RequestCard";
import SlotPicker from "../components/SlotPicker";
import UpcomingInterviews, { selectUpcoming } from "../components/UpcomingInterviews";
import PayModal from "../components/PayModal";
import TabBar from "../components/TabBar";

const EXPERIENCE_LEVELS = ["Fresher", "0-1 years", "1-3 years", "3-5 years", "5+ years"];

const EMPTY_FORM = {
  topic: "",
  experienceLevel: "Fresher",
  preferredSlot: "",
  notes: "",
};

/** Still in play vs finished - drives the two list tabs. */
const LIVE = ["AWAITING_PAYMENT", "PENDING", "SCHEDULED"];
const DONE = ["COMPLETED", "CANCELLED"];

export default function StudentDashboard() {
  const [tab, setTab] = useState("book");

  const [form, setForm] = useState(EMPTY_FORM);
  const [date, setDate] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payingFor, setPayingFor] = useState(null);

  useEffect(() => {
    api
      .myRequests()
      .then(setRequests)
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, []);

  const updateField = (e) => setForm((c) => ({ ...c, [e.target.name]: e.target.value }));

  async function submitRequest(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setFieldErrors({});

    if (!form.preferredSlot) {
      setSubmitting(false);
      setFieldErrors({ preferredSlot: "Pick a slot" });
      return;
    }

    try {
      const created = await api.createRequest(form);
      setForm(EMPTY_FORM);
      setDate("");
      setRequests(await api.myRequests());
      setPayingFor(created);
    } catch (error) {
      setFieldErrors(error.fieldErrors);
      setMessage({ type: "error", text: error.message });
    } finally {
      setSubmitting(false);
    }
  }

  async function cancelRequest(id) {
    if (!window.confirm("Cancel this request?")) return;
    try {
      await api.cancelRequest(id);
      setRequests(await api.myRequests());
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function afterPayment() {
    setPayingFor(null);
    setMessage({
      type: "success",
      text: "Thanks! We're checking your payment. Your slot is held in the meantime.",
    });
    setRequests(await api.myRequests());
    // Send them to the list, so they can see what state it's in.
    setTab("active");
  }

  const upcoming = selectUpcoming(requests);
  const live = requests.filter((r) => LIVE.includes(r.status));
  const done = requests.filter((r) => DONE.includes(r.status));
  const unpaid = requests.filter((r) => r.status === "AWAITING_PAYMENT").length;

  const actions = (request) => (
    <>
      {request.status === "AWAITING_PAYMENT" && (
        <button className="btn btn--primary" onClick={() => setPayingFor(request)}>
          Pay now
        </button>
      )}
      {LIVE.includes(request.status) && (
        <button className="btn btn--ghost" onClick={() => cancelRequest(request.id)}>
          Cancel request
        </button>
      )}
    </>
  );

  return (
    <>
      {payingFor && (
        <PayModal
          request={payingFor}
          onDone={afterPayment}
          onClose={() => {
            setPayingFor(null);
            setTab("active");
            setMessage({
              type: "error",
              text: "Your slot is held but not confirmed. Use “Pay now” to finish.",
            });
          }}
        />
      )}

      <UpcomingInterviews
        interviews={upcoming}
        otherPartyLabel="Interviewer"
        otherParty={(r) => r.mentor?.name ?? "—"}
      />

      <TabBar
        active={tab}
        onChange={setTab}
        tabs={[
          { key: "book", label: "Book an interview", icon: "＋" },
          { key: "active", label: "My interviews", icon: "📅", count: live.length, alert: unpaid > 0 },
          { key: "history", label: "History", icon: "🗂", count: done.length },
        ]}
      />

      {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}

      {tab === "book" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">New booking</span>
            <h2>What do you want to practise?</h2>
            <p>Tell us the round you&apos;re preparing for, then pick an hour that suits you.</p>
          </header>

          <form className="form" onSubmit={submitRequest}>
            <label className="field">
              <span>Topic</span>
              <input
                name="topic"
                value={form.topic}
                onChange={updateField}
                placeholder="Spring Boot backend round"
                required
              />
              {fieldErrors.topic && <small className="field__error">{fieldErrors.topic}</small>}
            </label>

            <label className="field">
              <span>Your experience</span>
              <select name="experienceLevel" value={form.experienceLevel} onChange={updateField}>
                {EXPERIENCE_LEVELS.map((level) => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </select>
            </label>

            <SlotPicker
              date={date}
              onDateChange={setDate}
              value={form.preferredSlot}
              onChange={(slot) => setForm((c) => ({ ...c, preferredSlot: slot }))}
              error={fieldErrors.preferredSlot}
            />

            <label className="field">
              <span>Anything we should know? (optional)</span>
              <textarea
                name="notes"
                value={form.notes}
                onChange={updateField}
                rows={3}
                placeholder="Final year student, weak on JPA relationships."
              />
            </label>

            <button
              className="btn btn--primary"
              type="submit"
              disabled={submitting || !form.preferredSlot}
            >
              {submitting ? (
                <>
                  <span className="spinner" /> Booking
                </>
              ) : (
                "Book slot & pay"
              )}
            </button>
          </form>
        </section>
      )}

      {tab === "active" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">In progress</span>
            <h2>
              My interviews <span className="count">{live.length}</span>
            </h2>
            <p>Everything booked but not finished yet.</p>
          </header>

          {loading && <p className="empty">Loading...</p>}
          {!loading && live.length === 0 && (
            <div className="empty">
              <p>Nothing booked right now.</p>
              <button className="btn btn--primary" onClick={() => setTab("book")}>
                Book an interview
              </button>
            </div>
          )}

          <div className="card-list">
            {live.map((r) => (
              <RequestCard key={r.id} request={r}>
                {actions(r)}
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
              History <span className="count">{done.length}</span>
            </h2>
            <p>Completed interviews and their scorecards, plus anything cancelled.</p>
          </header>

          {done.length === 0 && <p className="empty">No finished interviews yet.</p>}

          <div className="card-list">
            {done.map((r) => (
              <RequestCard key={r.id} request={r} />
            ))}
          </div>
        </section>
      )}
    </>
  );
}

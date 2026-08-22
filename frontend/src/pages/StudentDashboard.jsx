import { useEffect, useState } from "react";
import { api } from "../api/client";
import RequestCard from "../components/RequestCard";
import SlotPicker from "../components/SlotPicker";

const EXPERIENCE_LEVELS = ["Fresher", "0-1 years", "1-3 years", "3-5 years", "5+ years"];

const EMPTY_FORM = {
  topic: "",
  experienceLevel: "Fresher",
  preferredSlot: "",
  notes: "",
};

/**
 * The whole app, for a candidate: raise a request on the left, track your
 * requests on the right.
 *
 * There is no name or email field - the server takes those from your token.
 */
export default function StudentDashboard() {
  const [form, setForm] = useState(EMPTY_FORM);
  // The date lives outside the form because it only drives the slot grid -
  // what actually gets submitted is the chosen slot.
  const [date, setDate] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .myRequests()
      .then(setRequests)
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

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
      await api.createRequest(form);
      setForm(EMPTY_FORM);
      setDate("");
      setMessage({ type: "success", text: "Request sent. We'll line up an interviewer for you." });
      setRequests(await api.myRequests());
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

  return (
    <div className="sections">
      <section className="panel panel--student">
        <header className="panel__head">
          <span className="panel__tag">New request</span>
          <h2>What do you want to practise?</h2>
          <p>Tell us the round you&apos;re preparing for and when suits you.</p>
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
            onChange={(slot) => setForm((current) => ({ ...current, preferredSlot: slot }))}
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
            {submitting ? "Booking..." : "Book this slot"}
          </button>

          {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}
        </form>
      </section>

      <section className="panel panel--mentor">
        <header className="panel__head">
          <span className="panel__tag">Your interviews</span>
          <h2>
            Track your requests <span className="count">{requests.length}</span>
          </h2>
          <p>Once an interviewer is assigned you&apos;ll see the slot and joining link here.</p>
        </header>

        {loading && <p className="empty">Loading...</p>}
        {!loading && requests.length === 0 && (
          <p className="empty">You haven&apos;t requested an interview yet.</p>
        )}

        <div className="card-list">
          {requests.map((request) => (
            <RequestCard key={request.id} request={request}>
              {(request.status === "PENDING" || request.status === "SCHEDULED") && (
                <button className="btn btn--ghost" onClick={() => cancelRequest(request.id)}>
                  Cancel request
                </button>
              )}
            </RequestCard>
          ))}
        </div>
      </section>
    </div>
  );
}

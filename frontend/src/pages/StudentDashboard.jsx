import { useEffect, useState } from "react";
import { api } from "../api/client";
import RequestCard from "../components/RequestCard";

const EXPERIENCE_LEVELS = ["Fresher", "0-1 years", "1-3 years", "3-5 years", "5+ years"];

const EMPTY_FORM = {
  topic: "",
  experienceLevel: "Fresher",
  preferredDate: "",
  notes: "",
};

/**
 * The student's view. Notice there is no name/email field any more and no
 * "look up by email" box - the server already knows who you are from the token.
 */
export default function StudentDashboard() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [requests, setRequests] = useState([]);
  const [mentors, setMentors] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api.myRequests(), api.listMentors()])
      .then(([mine, mentorList]) => {
        setRequests(mine);
        setMentors(mentorList);
      })
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

    try {
      await api.createRequest(form);
      setForm(EMPTY_FORM);
      setMessage({ type: "success", text: "Request sent. A mentor will pick it up soon." });
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
          <span className="panel__tag">Request an interview</span>
          <h2>What do you want to practise?</h2>
          <p>A senior mentor picks it up and schedules a call with you.</p>
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

          <div className="form__row">
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

            <label className="field">
              <span>Preferred date</span>
              <input
                type="date"
                name="preferredDate"
                value={form.preferredDate}
                onChange={updateField}
                min={new Date().toISOString().slice(0, 10)}
                required
              />
              {fieldErrors.preferredDate && (
                <small className="field__error">{fieldErrors.preferredDate}</small>
              )}
            </label>
          </div>

          <label className="field">
            <span>Anything else the mentor should know? (optional)</span>
            <textarea
              name="notes"
              value={form.notes}
              onChange={updateField}
              rows={3}
              placeholder="Final year student, weak on JPA relationships."
            />
          </label>

          <button className="btn btn--primary" type="submit" disabled={submitting}>
            {submitting ? "Sending..." : "Request interview"}
          </button>

          {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}
        </form>

        <div className="divider" />

        <h3>Available mentors <span className="count">{mentors.length}</span></h3>
        <div className="mentor-list">
          {mentors.map((mentor) => (
            <div className="mentor-chip" key={mentor.userId}>
              <strong>{mentor.name}</strong>
              <span>{mentor.expertise}</span>
              <small>
                {mentor.yearsOfExperience} yrs
                {mentor.currentCompany && ` · ${mentor.currentCompany}`}
              </small>
            </div>
          ))}
        </div>
      </section>

      <section className="panel panel--mentor">
        <header className="panel__head">
          <span className="panel__tag">Your interviews</span>
          <h2>Track your requests</h2>
          <p>Everything you have asked for, newest first.</p>
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

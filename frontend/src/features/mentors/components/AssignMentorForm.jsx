import { useState } from "react";
import { useAvailableMentors } from "../useAvailableMentors";

/** Turn "2026-09-20T15:00:00" into the value a datetime-local input wants. */
function toInput(value) {
  return value ? value.slice(0, 16) : "";
}

/**
 * The admin attaching a mentor to one booking.
 *
 * <b>The dropdown is the mentors who declared this booking's exact hour</b> for
 * this kind of session - not every verified mentor. That is the point of the
 * availability feature: the person being assigned already agreed to the time,
 * rather than finding out afterwards.
 *
 * The override exists because reality intrudes - somebody agrees over the phone,
 * or a slot needs covering. It is deliberately a second, deliberate click with
 * the consequence spelled out, not a default.
 */
export default function AssignMentorForm({ request, mentors, onAssign, onCancel }) {
  const { mentors: available, error: loadError, loading } = useAvailableMentors(request.id);
  const [override, setOverride] = useState(false);
  const [mentorId, setMentorId] = useState("");
  const [scheduledAt, setScheduledAt] = useState(toInput(request.preferredSlot));
  const [meetingLink, setMeetingLink] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const mentoring = request.sessionType === "MENTORING";
  // Preselect the first mentor who offered it, so the common case is one click
  // and the safe option is the default. Derived rather than set in an effect.
  const selected = mentorId || (!override && available?.length ? String(available[0].mentorId) : "");
  const nobodyOffered = available !== null && available.length === 0;
  // Only when overriding do we fall back to the full verified list.
  const options = override
    ? mentors.map((m) => ({ id: m.userId, label: `${m.name} — ${m.expertise}` }))
    : (available ?? []).map((a) => ({ id: a.mentorId, label: `${a.mentorName} — offered ${a.label}` }));

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      await onAssign(request.id, {
        mentorId: Number(selected),
        scheduledAt: `${scheduledAt}:00`,
        // Blank means "create one for me" - the server generates the room.
        meetingLink: meetingLink.trim() || null,
        override,
      });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return <p className="empty">Checking who offered that hour...</p>;
  }

  return (
    <div className="accept-form">
      {loadError && <p className="notice notice--error">{loadError}</p>}

      {nobodyOffered && !override && (
        <div className="next-step next-step--warn">
          <strong>Nobody offered this hour</strong>
          <p>
            No verified mentor declared{" "}
            <strong>{new Date(request.preferredSlot).toLocaleString()}</strong> for a{" "}
            {mentoring ? "mentoring session" : "mock interview"}. Either wait for
            someone to add it, move the slot, or assign somebody anyway — but check
            with them first.
          </p>
          <button className="btn btn--ghost btn--sm" onClick={() => setOverride(true)}>
            Assign someone anyway
          </button>
        </div>
      )}

      {(!nobodyOffered || override) && (
        <>
          <label className="field">
            <span>
              {override ? "Assign to (any verified mentor)" : "Assign to (offered this hour)"}
            </span>
            <select value={selected} onChange={(e) => setMentorId(e.target.value)}>
              <option value="">Pick a mentor…</option>
              {options.map((o) => (
                <option key={o.id} value={o.id}>{o.label}</option>
              ))}
            </select>
            {!override && (
              <small className="field__hint">
                {available.length} mentor{available.length === 1 ? "" : "s"} offered this
                hour for a {mentoring ? "mentoring session" : "mock interview"}.
              </small>
            )}
            {override && (
              <small className="field__error">
                Overriding — this mentor has not agreed to the time. Confirm with them
                before assigning.
              </small>
            )}
          </label>

          <label className="field">
            <span>Slot (defaults to what the student asked for)</span>
            <input type="datetime-local" value={scheduledAt}
                   onChange={(e) => setScheduledAt(e.target.value)} />
            <small className="field__hint">
              Changing this away from an hour the mentor offered will need the
              override too — their availability is per hour.
            </small>
          </label>

          <label className="field">
            <span>Meeting link (optional)</span>
            <input value={meetingLink} onChange={(e) => setMeetingLink(e.target.value)}
                   placeholder="Leave blank — a room is created automatically" />
          </label>

          {error && <p className="notice notice--error">{error}</p>}

          <div className="accept-form__actions">
            <button className="btn btn--primary" onClick={submit}
                    disabled={busy || !selected || !scheduledAt}>
              {busy ? "Assigning..." : override ? "Assign anyway" : "Assign mentor"}
            </button>
            {override && (
              <button className="btn btn--ghost" onClick={() => setOverride(false)}
                      disabled={busy}>
                Back to available mentors
              </button>
            )}
            <button className="btn btn--ghost" onClick={onCancel} disabled={busy}>
              Cancel
            </button>
          </div>
        </>
      )}
    </div>
  );
}

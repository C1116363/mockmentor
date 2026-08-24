import { useState } from "react";

/** Turn "2026-09-20T15:00:00" into the value a datetime-local input wants. */
function toInput(value) {
  return value ? value.slice(0, 16) : "";
}

/**
 * The admin attaching a mentor to one request.
 *
 * Only verified mentors appear in the dropdown - the server refuses an
 * unapproved one anyway, so offering them would just produce an error.
 */
export default function AssignMentorForm({ request, mentors, onAssign, onCancel }) {
  const [mentorId, setMentorId] = useState(mentors[0]?.userId ?? "");
  const [scheduledAt, setScheduledAt] = useState(toInput(request.preferredSlot));
  const [meetingLink, setMeetingLink] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    try {
      await onAssign(request.id, {
        mentorId: Number(mentorId),
        scheduledAt: `${scheduledAt}:00`,
        // Blank means "create one for me" - the server generates the room.
        meetingLink: meetingLink.trim() || null,
      });
    } finally {
      setBusy(false);
    }
  }

  if (mentors.length === 0) {
    return (
      <p className="notice notice--error">
        No verified mentors yet. Approve a mentor profile first.
      </p>
    );
  }

  return (
    <div className="accept-form">
      <label className="field">
        <span>Assign to</span>
        <select value={mentorId} onChange={(e) => setMentorId(e.target.value)}>
          {mentors.map((m) => (
            <option key={m.userId} value={m.userId}>
              {m.name} — {m.expertise} ({m.yearsOfExperience} yrs)
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Slot (defaults to what the student asked for)</span>
        <input
          type="datetime-local"
          value={scheduledAt}
          onChange={(e) => setScheduledAt(e.target.value)}
        />
      </label>

      <label className="field">
        <span>Meeting link (optional)</span>
        <input
          value={meetingLink}
          onChange={(e) => setMeetingLink(e.target.value)}
          placeholder="Leave blank — a room is created automatically"
        />
        <small className="field__hint">
          A meeting room is created for you when you assign. Only fill this in if
          you want to use a link you made yourself.
        </small>
      </label>

      <div className="accept-form__actions">
        <button
          className="btn btn--primary"
          onClick={submit}
          disabled={busy || !mentorId || !scheduledAt}
        >
          {busy ? "Assigning..." : "Assign mentor"}
        </button>
        <button className="btn btn--ghost" onClick={onCancel} disabled={busy}>
          Cancel
        </button>
      </div>
    </div>
  );
}

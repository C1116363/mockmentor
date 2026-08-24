import { useSlots } from "../useSlots";

/** "2026-09-20" for a Date, in local time (toISOString would shift the day). */
function toDateInput(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

const TODAY = toDateInput(new Date());
const MAX_DATE = toDateInput(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));

/**
 * Pick a date, then an hour a mentor has offered on it.
 *
 * The grid is **not** generated - every button is an hour a verified mentor
 * declared for this kind of session. So an empty grid is a real answer, and the
 * copy says so rather than implying something failed.
 *
 * The server re-checks the choice on submit, so a stale grid can never book a
 * slot that has since been taken.
 */
export default function SlotPicker({ date, onDateChange, value, onChange, error, sessionType }) {
  const { slots, loading, error: loadError } = useSlots(date, sessionType);

  const anyAvailable = slots.some((s) => s.available);
  const mentoring = sessionType === "MENTORING";

  return (
    <div className="slotpick">
      <label className="field">
        <span>Pick a date</span>
        <input
          type="date"
          value={date}
          min={TODAY}
          max={MAX_DATE}
          onChange={(event) => {
            onDateChange(event.target.value);
            onChange(""); // a slot from the old day is meaningless
          }}
          required
        />
      </label>

      <div className="field">
        <span>
          Pick a time — each {mentoring ? "session" : "interview"} runs for 1 hour
        </span>
        <small className="field__hint">
          Only hours a mentor has offered appear here, and bookings need a
          day&apos;s notice.
        </small>

        {!date && <p className="empty">Choose a date first.</p>}
        {date && loading && <p className="empty">Loading slots...</p>}
        {date && loadError && <p className="notice notice--error">{loadError}</p>}

        {date && !loading && !loadError && slots.length === 0 && (
          <p className="empty">
            No mentor has offered {mentoring ? "mentoring" : "interview"} hours on this
            day yet. Try another date — or check back, mentors add their hours as they
            go.
          </p>
        )}

        {date && !loading && !loadError && slots.length > 0 && (
          <>
            <div className="slots">
              {slots.map((slot) => {
                const selected = value === slot.start;
                return (
                  <button
                    key={slot.start}
                    type="button"
                    className={`slot ${selected ? "slot--on" : ""} ${slot.available ? "" : "slot--off"}`}
                    disabled={!slot.available}
                    title={slot.unavailableReason
                      ?? `${slot.label} – 1 hour · ${slot.remaining} place(s) left`}
                    aria-pressed={selected}
                    onClick={() => onChange(slot.start)}
                  >
                    {slot.label}
                  </button>
                );
              })}
            </div>

            {!anyAvailable && (
              <p className="empty">
                Every offered hour on this day is either taken or inside the
                24-hour notice period. Try a later date.
              </p>
            )}
          </>
        )}

        {error && <small className="field__error">{error}</small>}
      </div>
    </div>
  );
}

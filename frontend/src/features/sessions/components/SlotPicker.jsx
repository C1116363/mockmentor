import { useSlots } from "../useSlots";

/** "2026-09-20" for a Date, in local time (toISOString would shift the day). */
function toDateInput(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

const TODAY = toDateInput(new Date());
const MAX_DATE = toDateInput(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));

/**
 * Pick a date, then a one-hour slot on that date.
 *
 * Availability comes from the server, not from the browser - it knows which
 * slots have passed and which are full. The server re-checks the choice on
 * submit anyway, so a stale grid can never book a bad slot.
 */
export default function SlotPicker({ date, onDateChange, value, onChange, error }) {
  const { slots, loading, error: loadError } = useSlots(date);


  const anyAvailable = slots.some((s) => s.available);

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
        <span>Pick a time — each interview runs for 1 hour</span>

        {!date && <p className="empty">Choose a date first.</p>}
        {date && loading && <p className="empty">Loading slots...</p>}
        {date && loadError && <p className="notice notice--error">{loadError}</p>}

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
                    title={slot.unavailableReason ?? `${slot.label} – 1 hour`}
                    aria-pressed={selected}
                    onClick={() => onChange(slot.start)}
                  >
                    {slot.label}
                  </button>
                );
              })}
            </div>

            {!anyAvailable && (
              <p className="empty">Nothing left on this day. Try another date.</p>
            )}
          </>
        )}

        {error && <small className="field__error">{error}</small>}
      </div>
    </div>
  );
}

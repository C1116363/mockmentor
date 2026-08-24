import { useState } from "react";

/** 09:00 to 20:00 inclusive - the last bookable hour starts at 8 PM and ends at 9. */
const HOURS = Array.from({ length: 12 }, (_, i) => 9 + i);

const label = (hour) => {
  const suffix = hour < 12 ? "AM" : "PM";
  const twelve = hour % 12 === 0 ? 12 : hour % 12;
  return `${twelve}:00 ${suffix}`;
};

/** "2026-09-20" for a Date, in local time - toISOString would shift the day. */
function toDateInput(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/**
 * Tomorrow, not today.
 *
 * An hour needs 24 hours' notice, so nothing on today's date could ever be
 * accepted - offering the date at all would just invite a rejection. Same reason
 * the hint says "from tomorrow" rather than explaining the rule after the fact.
 */
const MIN_DATE = toDateInput(new Date(Date.now() + 24 * 60 * 60 * 1000));
const MAX_DATE = toDateInput(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));

/**
 * A mentor declaring the hours they are free.
 *
 * Bulk by day, because that is how anyone thinks about their own calendar -
 * "Tuesday afternoon", not "3 PM, then 4 PM, then 5 PM". The server accepts what
 * it can and reports the rest, so ticking a whole afternoon never fails outright
 * because one hour is already booked.
 */
export default function AvailabilityPlanner({ onDeclare, existing }) {
  const [date, setDate] = useState("");
  const [picked, setPicked] = useState([]);
  const [forInterviews, setForInterviews] = useState(true);
  const [forMentoring, setForMentoring] = useState(true);
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  // What is already declared on the chosen day, so an hour is not offered twice
  // and a booked one cannot be tampered with.
  const onThisDay = date
    ? existing.filter((a) => a.slotStart.startsWith(date))
    : [];
  const statusOf = (hour) =>
    onThisDay.find((a) => new Date(a.slotStart).getHours() === hour)?.status ?? null;

  const toggle = (hour) =>
    setPicked((c) => (c.includes(hour) ? c.filter((h) => h !== hour) : [...c, hour]));

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const outcome = await onDeclare({
        date,
        hours: picked,
        forInterviews,
        forMentoring,
        note: note.trim() || null,
      });
      setResult(outcome);
      setPicked([]);
      setNote("");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const noKindPicked = !forInterviews && !forMentoring;

  return (
    <form className="form send-form" onSubmit={submit}>
      <label className="field">
        <span>Which day?</span>
        <input type="date" value={date} min={MIN_DATE} max={MAX_DATE}
               onChange={(e) => { setDate(e.target.value); setPicked([]); setResult(null); }}
               required />
        <small className="field__hint">
          From tomorrow onwards — an hour needs a day&apos;s notice so a student can
          book it and an admin can arrange it.
        </small>
      </label>

      {date && (
        <div className="field">
          <span>Which hours are you free? <em>(each is one session)</em></span>
          <div className="hours">
            {HOURS.map((hour) => {
              const status = statusOf(hour);
              const selected = picked.includes(hour);
              const booked = status === "BOOKED";
              return (
                <button
                  key={hour}
                  type="button"
                  className={`hour ${selected ? "hour--on" : ""} `
                    + `${status === "OPEN" ? "hour--offered" : ""} ${booked ? "hour--booked" : ""}`}
                  disabled={booked}
                  aria-pressed={selected}
                  title={booked
                    ? "A student is booked into this hour"
                    : status === "OPEN"
                      ? "Already offered — tick to change what you'll take in it"
                      : label(hour)}
                  onClick={() => toggle(hour)}
                >
                  {label(hour)}
                  {status === "OPEN" && <span className="hour__flag">offered</span>}
                  {booked && <span className="hour__flag">booked</span>}
                </button>
              );
            })}
          </div>
          <small className="field__hint">
            Hours you already offered are highlighted — ticking one again just
            updates what you&apos;ll take in it. Booked hours can&apos;t be changed here.
          </small>
        </div>
      )}

      <fieldset className="field audience">
        <span>What will you take in these hours?</span>
        <label className="check">
          <input type="checkbox" checked={forInterviews}
                 onChange={(e) => setForInterviews(e.target.checked)} />
          <span>
            Mock interviews
            <small className="field__hint"> — under real pressure, ending in a scorecard</small>
          </span>
        </label>
        <label className="check">
          <input type="checkbox" checked={forMentoring}
                 onChange={(e) => setForMentoring(e.target.checked)} />
          <span>
            Mentoring sessions
            <small className="field__hint"> — a discussion, no ratings</small>
          </span>
        </label>
        {noKindPicked && (
          <small className="field__error">
            Pick at least one — an hour you&apos;ll take neither in isn&apos;t availability.
          </small>
        )}
      </fieldset>

      <label className="field">
        <span>Note for the admin <em>(optional)</em></span>
        <input value={note} onChange={(e) => setNote(e.target.value)}
               placeholder="Prefer backend topics this week" />
      </label>

      {error && <p className="notice notice--error">{error}</p>}

      {/* The server's message is the interesting half: it names the hours it
          skipped and why. Showing only "4 hours offered" would hide the two that
          were not. */}
      {result?.message && (
        <p className={`notice notice--${result.count > 0 ? "success" : "error"}`}>
          {result.message}
        </p>
      )}

      <button className="btn btn--primary" type="submit"
              disabled={busy || !date || picked.length === 0 || noKindPicked}>
        {busy
          ? "Saving..."
          : `Offer ${picked.length || "these"} hour${picked.length === 1 ? "" : "s"}`}
      </button>
    </form>
  );
}

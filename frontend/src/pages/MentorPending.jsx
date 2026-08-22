/** Shown while an admin is reviewing a mentor's profile. */
export default function MentorPending({ profile, onRefresh }) {
  return (
    <div className="panel notice-panel">
      <div className="waiting">
        <span className="waiting__dot" />
        <span className="waiting__dot" />
        <span className="waiting__dot" />
      </div>

      <h2>Your profile is under verification</h2>
      <p>
        Thanks {profile.fullName?.split(" ")[0]} — an admin is checking your
        details. You&apos;ll get access to the interview queue as soon as
        you&apos;re approved. This usually takes about a day.
      </p>

      <dl className="card__facts submitted">
        <div>
          <dt>Submitted</dt>
          <dd>{profile.submittedAt ? new Date(profile.submittedAt).toLocaleString() : "—"}</dd>
        </div>
        <div>
          <dt>Company</dt>
          <dd>{profile.currentCompany}</dd>
        </div>
        <div>
          <dt>Experience</dt>
          <dd>{profile.yearsOfExperience} years</dd>
        </div>
        <div>
          <dt>Aadhaar</dt>
          <dd>{profile.aadhaarNumberMasked}</dd>
        </div>
      </dl>

      <button className="btn btn--ghost" onClick={onRefresh}>
        Check again
      </button>
    </div>
  );
}

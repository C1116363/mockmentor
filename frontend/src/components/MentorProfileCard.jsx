import { useState } from "react";

const BADGE = {
  INCOMPLETE: { cls: "badge--cancelled", label: "NOT SUBMITTED" },
  PENDING: { cls: "badge--pending", label: "AWAITING REVIEW" },
  APPROVED: { cls: "badge--completed", label: "VERIFIED" },
  REJECTED: { cls: "badge--rejected", label: "REJECTED" },
};

function when(value) {
  return value ? new Date(value).toLocaleDateString(undefined, { dateStyle: "medium" }) : "—";
}

/**
 * One mentor, in whatever state they're in.
 *
 * The same card serves the review queue and the verified list - only the
 * actions differ, so they're passed in as children.
 */
export default function MentorProfileCard({ profile, children }) {
  const [showSensitive, setShowSensitive] = useState(false);
  const badge = BADGE[profile.verificationStatus] ?? BADGE.INCOMPLETE;
  const submitted = profile.verificationStatus !== "INCOMPLETE";

  return (
    <article className="card">
      <header className="card__head">
        <div>
          <h4 className="card__title">{profile.fullName}</h4>
          <p className="card__sub">
            {submitted
              ? `${profile.currentRoleTitle ?? "—"} at ${profile.currentCompany ?? "—"} · ${profile.yearsOfExperience} yrs`
              : "Hasn't filled in their profile yet"}
          </p>
        </div>
        <span className={`badge ${badge.cls}`}>{badge.label}</span>
      </header>

      <dl className="card__facts">
        <div><dt>Email</dt><dd>{profile.email}</dd></div>
        {submitted && <div><dt>Phone</dt><dd>{profile.phoneNumber ?? "—"}</dd></div>}
        {submitted && (
          <div>
            <dt>Qualification</dt>
            <dd>{profile.highestQualification ?? "—"}</dd>
          </div>
        )}
        {submitted && (
          <div>
            <dt>University</dt>
            <dd>
              {profile.university ?? "—"}
              {profile.graduationYear ? ` (${profile.graduationYear})` : ""}
            </dd>
          </div>
        )}
        {profile.verificationStatus === "APPROVED" && (
          <>
            <div><dt>Verified on</dt><dd>{when(profile.reviewedAt)}</dd></div>
            <div><dt>Verified by</dt><dd>{profile.reviewedBy ?? "—"}</dd></div>
          </>
        )}
      </dl>

      {profile.verificationStatus === "REJECTED" && profile.rejectionReason && (
        <div className="card__feedback">
          <strong>Rejection reason</strong>
          <p>{profile.rejectionReason}</p>
        </div>
      )}

      {profile.expertise && <div className="tags-row">{profile.expertise}</div>}
      {profile.bio && <p className="card__notes">{profile.bio}</p>}

      {profile.linkedinUrl && (
        <a className="card__link" href={profile.linkedinUrl} target="_blank" rel="noreferrer">
          Open LinkedIn &rarr;
        </a>
      )}

      {submitted && (
        <>
          {showSensitive && (
            <dl className="card__facts card__facts--sensitive">
              <div><dt>Aadhaar</dt><dd>{profile.aadhaarNumberMasked ?? "—"}</dd></div>
              <div><dt>PAN</dt><dd>{profile.panNumber ?? "—"}</dd></div>
              <div><dt>Account holder</dt><dd>{profile.bankAccountHolder ?? "—"}</dd></div>
              <div><dt>Account no.</dt><dd>{profile.bankAccountNumberMasked ?? "—"}</dd></div>
              <div><dt>IFSC</dt><dd>{profile.bankIfsc ?? "—"}</dd></div>
              <div><dt>Bank</dt><dd>{profile.bankName ?? "—"}</dd></div>
            </dl>
          )}

          <button className="linkish" onClick={() => setShowSensitive((v) => !v)}>
            {showSensitive ? "Hide KYC & bank details" : "Show KYC & bank details"}
          </button>
        </>
      )}

      {children}
    </article>
  );
}

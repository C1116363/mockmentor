import { useState } from "react";
import { useMentorProfile } from "../features/mentors/useMentorProfile";

const EMPTY = {
  expertise: "",
  yearsOfExperience: 5,
  currentCompany: "",
  currentRoleTitle: "",
  bio: "",
  linkedinUrl: "",
  highestQualification: "",
  university: "",
  graduationYear: "",
  phoneNumber: "",
  aadhaarNumber: "",
  panNumber: "",
  bankAccountHolder: "",
  bankAccountNumber: "",
  bankIfsc: "",
  bankName: "",
};

/**
 * The form a mentor fills in to get verified.
 *
 * Shown when their profile is INCOMPLETE, and again after a REJECTED decision
 * so they can fix whatever the admin flagged.
 */
export default function MentorProfileForm({ profile, onSubmitted }) {
  // load:false - the profile arrives as a prop, so there is nothing to fetch.
  const { submit: submitProfile } = useMentorProfile({ load: false });

  const [form, setForm] = useState({
    ...EMPTY,
    // Prefill anything already saved (i.e. after a rejection).
    ...Object.fromEntries(
      Object.keys(EMPTY)
        .filter((k) => profile?.[k] !== null && profile?.[k] !== undefined && profile?.[k] !== "")
        .map((k) => [k, profile[k]])
    ),
    // Masked values can't be re-submitted, so make them retype these two.
    aadhaarNumber: "",
    bankAccountNumber: "",
  });

  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const rejected = profile?.verificationStatus === "REJECTED";

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});

    try {
      const saved = await submitProfile({
        ...form,
        yearsOfExperience: Number(form.yearsOfExperience),
        graduationYear: Number(form.graduationYear),
      });
      onSubmitted(saved);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setBusy(false);
    }
  }

  const field = (name, label, props = {}) => (
    <label className="field">
      <span>{label}</span>
      <input name={name} value={form[name]} onChange={update} {...props} />
      {fieldErrors[name] && <small className="field__error">{fieldErrors[name]}</small>}
    </label>
  );

  return (
    <div className="profile-form">
      <header className="panel__head">
        <span className="panel__tag">Mentor onboarding</span>
        <h2>{rejected ? "Fix your profile and resubmit" : "Complete your profile"}</h2>
        <p>
          An admin checks these details before you can start taking interviews.
          It usually takes a day.
        </p>
      </header>

      {rejected && (
        <div className="notice notice--error">
          <strong>Your last submission was rejected.</strong>
          <br />
          {profile.rejectionReason}
        </div>
      )}

      <form className="form" onSubmit={submit}>
        <fieldset className="fs">
          <legend>Professional</legend>
          {field("expertise", "Areas you can interview on", {
            placeholder: "Java, Spring Boot, System Design",
            required: true,
          })}
          <div className="form__row">
            {field("currentCompany", "Current company", { placeholder: "Flipkart", required: true })}
            {field("currentRoleTitle", "Your designation", {
              placeholder: "Senior Software Engineer",
              required: true,
            })}
          </div>
          <div className="form__row">
            {field("yearsOfExperience", "Years of experience", {
              type: "number",
              min: 3,
              max: 50,
              required: true,
            })}
            {field("linkedinUrl", "LinkedIn (optional)", {
              placeholder: "https://linkedin.com/in/you",
            })}
          </div>
          <label className="field">
            <span>Short bio shown to candidates (optional)</span>
            <textarea name="bio" rows={2} value={form.bio} onChange={update} />
          </label>
        </fieldset>

        <fieldset className="fs">
          <legend>Education</legend>
          {field("highestQualification", "Highest qualification", {
            placeholder: "B.Tech Computer Science",
            required: true,
          })}
          <div className="form__row">
            {field("university", "College / university", { placeholder: "NIT Trichy", required: true })}
            {field("graduationYear", "Graduation year", {
              type: "number",
              min: 1960,
              max: 2100,
              placeholder: "2016",
              required: true,
            })}
          </div>
        </fieldset>

        <fieldset className="fs">
          <legend>Identity &amp; contact</legend>
          <p className="fs__note">
            Used only to verify you are who you say you are. Stored securely and
            never shown to candidates.
          </p>
          <div className="form__row">
            {field("phoneNumber", "Mobile number", {
              placeholder: "9876543210",
              inputMode: "numeric",
              maxLength: 10,
              required: true,
            })}
            {field("aadhaarNumber", "Aadhaar number", {
              placeholder: "12 digits",
              inputMode: "numeric",
              maxLength: 12,
              required: true,
            })}
          </div>
          {field("panNumber", "PAN", {
            placeholder: "ABCDE1234F",
            maxLength: 10,
            style: { textTransform: "uppercase" },
            required: true,
          })}
        </fieldset>

        <fieldset className="fs">
          <legend>Bank details</legend>
          <p className="fs__note">Where we send your payout after each interview.</p>
          {field("bankAccountHolder", "Account holder name", {
            placeholder: "As printed on your passbook",
            required: true,
          })}
          <div className="form__row">
            {field("bankAccountNumber", "Account number", {
              placeholder: "9 to 18 digits",
              inputMode: "numeric",
              maxLength: 18,
              required: true,
            })}
            {field("bankIfsc", "IFSC", {
              placeholder: "HDFC0001234",
              maxLength: 11,
              style: { textTransform: "uppercase" },
              required: true,
            })}
          </div>
          {field("bankName", "Bank name", { placeholder: "HDFC Bank", required: true })}
        </fieldset>

        <button className="btn btn--primary" type="submit" disabled={busy}>
          {busy ? "Submitting..." : rejected ? "Resubmit for verification" : "Submit for verification"}
        </button>

        {error && <p className="notice notice--error">{error}</p>}
      </form>
    </div>
  );
}

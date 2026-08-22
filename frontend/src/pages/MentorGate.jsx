import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import MentorProfileForm from "./MentorProfileForm";
import MentorPending from "./MentorPending";
import MentorDashboard from "./MentorDashboard";

/**
 * Decides which mentor screen to show, based on verification status:
 *
 *   INCOMPLETE / REJECTED -> the profile form
 *   PENDING               -> "under verification"
 *   APPROVED              -> the actual dashboard
 *
 * This is presentation only. The server refuses mentor actions from an
 * unapproved account regardless of what the browser decides to render.
 */
export default function MentorGate() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    return api
      .myMentorProfile()
      .then((p) => {
        setProfile(p);
        setError(null);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <p className="empty">Loading your profile...</p>;
  if (error) return <p className="notice notice--error">{error}</p>;

  switch (profile.verificationStatus) {
    case "APPROVED":
      return <MentorDashboard />;
    case "PENDING":
      return <MentorPending profile={profile} onRefresh={load} />;
    default:
      // INCOMPLETE or REJECTED - both need the form.
      return <MentorProfileForm profile={profile} onSubmitted={setProfile} />;
  }
}

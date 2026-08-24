import MentorProfileForm from "./MentorProfileForm";
import MentorPending from "./MentorPending";
import MentorDashboard from "./MentorDashboard";
import { useMentorProfile } from "../features/mentors/useMentorProfile";

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
  const { profile, loading, error, reload } = useMentorProfile();

  if (loading) return <p className="empty">Loading your profile...</p>;
  if (error) return <p className="notice notice--error">{error}</p>;

  switch (profile.verificationStatus) {
    case "APPROVED":
      return <MentorDashboard />;
    case "PENDING":
      return <MentorPending profile={profile} onRefresh={reload} />;
    default:
      // INCOMPLETE or REJECTED - both need the form.
      return <MentorProfileForm profile={profile} onSubmitted={reload} />;
  }
}

import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import StatusBadge from "../components/StatusBadge";
import RequestCard from "../components/RequestCard";
import AssignMentorForm from "../components/AssignMentorForm";
import MentorProfileCard from "../components/MentorProfileCard";
import PaymentReviewCard from "../components/PaymentReviewCard";
import { useSectionNav } from "../nav/SectionNav";

const STAT_LABELS = {
  awaitingPayment: "Unpaid",
  paymentsToCheck: "To verify",
  students: "Candidates",
  mentors: "Mentors",
  admins: "Admins",
  totalRequests: "Requests",
  pending: "Unassigned",
  scheduled: "Scheduled",
  completed: "Completed",
  cancelled: "Cancelled",
};

/** Everything an admin does: verify mentors, and match students to mentors. */
export default function AdminDashboard() {
  const { active: tab, register, go: setTab } = useSectionNav();
  const [stats, setStats] = useState({});
  const [profiles, setProfiles] = useState([]);  // every mentor profile, all statuses
  const [payments, setPayments] = useState([]);  // payments awaiting verification
  const [mentorFilter, setMentorFilter] = useState("PENDING");
  const [unassigned, setUnassigned] = useState([]); // student requests with no mentor
  const [mentors, setMentors] = useState([]);    // verified mentors, for the dropdown
  const [users, setUsers] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);
  const [assigning, setAssigning] = useState(null);

  const reload = useCallback(async () => {
    const [s, q, p, m, u, r, pay] = await Promise.all([
      api.adminStats(),
      api.allMentorProfiles(),
      api.adminPendingRequests(),
      api.listMentors(),
      api.adminUsers(),
      api.adminRequests(),
      api.pendingPayments(),
    ]);
    setStats(s);
    setProfiles(q);
    setUnassigned(p);
    setMentors(m);
    setUsers(u);
    setRequests(r);
    setPayments(pay);
  }, []);

  useEffect(() => {
    reload()
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, [reload]);

  async function assign(requestId, payload) {
    try {
      const updated = await api.assignMentor(requestId, payload);
      setAssigning(null);
      setMessage({
        type: "success",
        text: `Assigned to ${updated.mentor.name}. The student can see it now.`,
      });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function verifyPayment(payment) {
    try {
      await api.verifyPayment(payment.id);
      setMessage({
        type: "success",
        text: `Payment confirmed. ${payment.studentName}'s interview is now open to mentors.`,
      });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function rejectPayment(payment) {
    const reason = window.prompt(`Why are you rejecting this payment from ${payment.studentName}?`);
    if (!reason) return;
    try {
      await api.rejectPayment(payment.id, reason);
      setMessage({ type: "success", text: "Payment rejected. The student can send new proof." });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function approve(profile) {
    try {
      await api.approveMentor(profile.id);
      setMessage({ type: "success", text: `${profile.fullName} is now verified.` });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function reject(profile) {
    const reason = window.prompt(`Why are you rejecting ${profile.fullName}?`);
    if (!reason) return;
    try {
      await api.rejectMentor(profile.id, reason);
      setMessage({ type: "success", text: `${profile.fullName} was rejected.` });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  async function toggleActive(user) {
    try {
      if (user.active) await api.deactivateUser(user.id);
      else await api.activateUser(user.id);
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  const pendingCount = profiles.filter((p) => p.verificationStatus === "PENDING").length;

  // Registered before the early return below - a hook after a conditional
  // return would change hook order between renders and React would throw.
  useEffect(() => {
    register(
      [
        { key: "payments", label: "Payments", icon: "💳", count: payments.length, alert: true },
        { key: "requests", label: "Assign", icon: "🔗", count: unassigned.length, alert: true },
        { key: "verify", label: "Mentors", icon: "🧭", count: pendingCount, alert: true },
        { key: "users", label: "Users", icon: "👥" },
        { key: "all", label: "All requests", icon: "🗂" },
      ],
      "payments"
    );
  }, [register, payments.length, unassigned.length, pendingCount]);

  if (loading) return <p className="empty">Loading admin data...</p>;

  const shown = profiles.filter((p) => p.verificationStatus === mentorFilter);

  const MENTOR_FILTERS = [
    { key: "PENDING", label: "Awaiting review" },
    { key: "APPROVED", label: "Verified" },
    { key: "REJECTED", label: "Rejected" },
    { key: "INCOMPLETE", label: "Not submitted" },
  ];

  return (
    <div className="admin">
      {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}

      <div className="stat-grid">
        {Object.entries(stats).map(([key, value]) => (
          <div className="stat" key={key}>
            <span className="stat__value">{value}</span>
            <span className="stat__label">{STAT_LABELS[key] ?? key}</span>
          </div>
        ))}
      </div>


      {tab === "payments" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Money in</span>
            <h2>
              Payments to verify <span className="count">{payments.length}</span>
            </h2>
            <p>
              Students who have sent a UPI reference and screenshot. Check the UTR
              against your bank, then confirm — that is what releases the booking
              to mentors.
            </p>
          </header>

          {payments.length === 0 && <p className="empty">No payments waiting to be checked.</p>}

          <div className="card-list">
            {payments.map((p) => (
              <PaymentReviewCard
                key={p.id}
                payment={p}
                onVerify={verifyPayment}
                onReject={rejectPayment}
              />
            ))}
          </div>
        </section>
      )}

      {tab === "requests" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Sent by students</span>
            <h2>
              Waiting for a mentor <span className="count">{unassigned.length}</span>
            </h2>
            <p>
              Pick a verified mentor for each request. The slot defaults to the
              one the student asked for, and you can change it.
            </p>
          </header>

          {unassigned.length === 0 && (
            <p className="empty">Every request has a mentor. Nothing to assign.</p>
          )}

          <div className="card-list">
            {unassigned.map((r) => (
              <RequestCard key={r.id} request={r}>
                {assigning === r.id ? (
                  <AssignMentorForm
                    request={r}
                    mentors={mentors}
                    onAssign={assign}
                    onCancel={() => setAssigning(null)}
                  />
                ) : (
                  <button className="btn btn--primary" onClick={() => setAssigning(r.id)}>
                    Assign a mentor
                  </button>
                )}
              </RequestCard>
            ))}
          </div>
        </section>
      )}

      {tab === "verify" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Mentor directory</span>
            <h2>
              Mentors <span className="count">{profiles.length}</span>
            </h2>
            <p>
              Everyone who signed up to give interviews. Approve the ones waiting,
              and check on the ones you already verified.
            </p>
          </header>

          <div className="chips-row">
            {MENTOR_FILTERS.map((f) => {
              const n = profiles.filter((p) => p.verificationStatus === f.key).length;
              return (
                <button
                  key={f.key}
                  className={`chip-sm ${mentorFilter === f.key ? "chip-sm--on" : ""}`}
                  onClick={() => setMentorFilter(f.key)}
                >
                  {f.label} <span className="chip-sm__n">{n}</span>
                </button>
              );
            })}
          </div>

          {shown.length === 0 && (
            <p className="empty">
              {mentorFilter === "PENDING"
                ? "Nothing waiting for review."
                : "No mentors in this state."}
            </p>
          )}

          <div className="card-list">
            {shown.map((p) => (
              <MentorProfileCard key={p.id} profile={p}>
                {p.verificationStatus === "PENDING" && (
                  <div className="accept-form__actions">
                    <button className="btn btn--primary" onClick={() => approve(p)}>Approve</button>
                    <button className="btn btn--ghost" onClick={() => reject(p)}>Reject</button>
                  </div>
                )}
              </MentorProfileCard>
            ))}
          </div>
        </section>
      )}

      {tab === "users" && (
        <section className="panel">
          <header className="panel__head">
            <h2>All users <span className="count">{users.length}</span></h2>
            <p>Deactivating an account blocks that person from logging in.</p>
          </header>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th /></tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.fullName}</td>
                    <td className="muted">{u.email}</td>
                    <td><span className={`role role--${u.role.toLowerCase()}`}>{u.role}</span></td>
                    <td>{u.active ? "Active" : "Deactivated"}</td>
                    <td>
                      <button className="btn btn--ghost btn--sm" onClick={() => toggleActive(u)}>
                        {u.active ? "Deactivate" : "Activate"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === "all" && (
        <section className="panel">
          <header className="panel__head">
            <h2>All requests <span className="count">{requests.length}</span></h2>
          </header>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Topic</th><th>Candidate</th><th>Mentor</th><th>Slot</th><th>Status</th></tr>
              </thead>
              <tbody>
                {requests.map((r) => (
                  <tr key={r.id}>
                    <td>{r.topic}</td>
                    <td className="muted">{r.student.fullName}</td>
                    <td className="muted">{r.mentor?.name ?? "—"}</td>
                    <td className="muted">
                      {r.preferredSlot ? new Date(r.preferredSlot).toLocaleString() : "—"}
                    </td>
                    <td><StatusBadge status={r.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}

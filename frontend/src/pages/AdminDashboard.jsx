import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import StatusBadge from "../components/StatusBadge";

const STAT_LABELS = {
  students: "Candidates",
  mentors: "Mentors",
  admins: "Admins",
  totalRequests: "Requests",
  pending: "Pending",
  scheduled: "Scheduled",
  completed: "Completed",
  cancelled: "Cancelled",
};

const TABS = [
  { key: "verify", label: "Mentor verification" },
  { key: "users", label: "Users" },
  { key: "requests", label: "Requests" },
];

/** Everything an admin does, including verifying mentors. */
export default function AdminDashboard() {
  const [tab, setTab] = useState("verify");
  const [stats, setStats] = useState({});
  const [queue, setQueue] = useState([]);
  const [users, setUsers] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);
  const [expanded, setExpanded] = useState(null);

  const reload = useCallback(async () => {
    const [s, q, u, r] = await Promise.all([
      api.adminStats(),
      api.pendingMentorProfiles(),
      api.adminUsers(),
      api.adminRequests(),
    ]);
    setStats(s);
    setQueue(q);
    setUsers(u);
    setRequests(r);
  }, []);

  useEffect(() => {
    reload()
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, [reload]);

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

  if (loading) return <p className="empty">Loading admin data...</p>;

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

      <div className="tabs tabs--admin">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab ${tab === t.key ? "tab--on" : ""}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
            {t.key === "verify" && queue.length > 0 && <span className="badge-dot">{queue.length}</span>}
          </button>
        ))}
      </div>

      {tab === "verify" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Awaiting review</span>
            <h2>
              Mentor verification <span className="count">{queue.length}</span>
            </h2>
            <p>
              Check the details against their documents, then approve or reject.
              Aadhaar and account numbers are masked to the last 4 digits.
            </p>
          </header>

          {queue.length === 0 && <p className="empty">Nothing waiting for review.</p>}

          <div className="card-list">
            {queue.map((p) => (
              <article className="card" key={p.id}>
                <header className="card__head">
                  <div>
                    <h4 className="card__title">{p.fullName}</h4>
                    <p className="card__sub">
                      {p.currentRoleTitle} at {p.currentCompany} · {p.yearsOfExperience} yrs
                    </p>
                  </div>
                  <span className="badge badge--pending">PENDING</span>
                </header>

                <dl className="card__facts">
                  <div><dt>Email</dt><dd>{p.email}</dd></div>
                  <div><dt>Phone</dt><dd>{p.phoneNumber}</dd></div>
                  <div><dt>Qualification</dt><dd>{p.highestQualification}</dd></div>
                  <div><dt>University</dt><dd>{p.university} ({p.graduationYear})</dd></div>
                </dl>

                {expanded === p.id && (
                  <dl className="card__facts card__facts--sensitive">
                    <div><dt>Aadhaar</dt><dd>{p.aadhaarNumberMasked}</dd></div>
                    <div><dt>PAN</dt><dd>{p.panNumber}</dd></div>
                    <div><dt>Account holder</dt><dd>{p.bankAccountHolder}</dd></div>
                    <div><dt>Account no.</dt><dd>{p.bankAccountNumberMasked}</dd></div>
                    <div><dt>IFSC</dt><dd>{p.bankIfsc}</dd></div>
                    <div><dt>Bank</dt><dd>{p.bankName}</dd></div>
                  </dl>
                )}

                <button
                  className="linkish"
                  onClick={() => setExpanded(expanded === p.id ? null : p.id)}
                >
                  {expanded === p.id ? "Hide KYC & bank details" : "Show KYC & bank details"}
                </button>

                {p.expertise && <div className="tags-row">{p.expertise}</div>}
                {p.bio && <p className="card__notes">{p.bio}</p>}

                {p.linkedinUrl && (
                  <a className="card__link" href={p.linkedinUrl} target="_blank" rel="noreferrer">
                    Open LinkedIn &rarr;
                  </a>
                )}

                <div className="accept-form__actions">
                  <button className="btn btn--primary" onClick={() => approve(p)}>
                    Approve
                  </button>
                  <button className="btn btn--ghost" onClick={() => reject(p)}>
                    Reject
                  </button>
                </div>
              </article>
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

      {tab === "requests" && (
        <section className="panel">
          <header className="panel__head">
            <h2>All requests <span className="count">{requests.length}</span></h2>
          </header>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Topic</th><th>Candidate</th><th>Interviewer</th><th>Slot</th><th>Status</th></tr>
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

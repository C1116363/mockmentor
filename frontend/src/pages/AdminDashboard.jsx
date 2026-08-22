import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import StatusBadge from "../components/StatusBadge";

const STAT_LABELS = {
  students: "Students",
  mentors: "Mentors",
  admins: "Admins",
  totalRequests: "Total requests",
  pending: "Pending",
  scheduled: "Scheduled",
  completed: "Completed",
  cancelled: "Cancelled",
};

/** Admin-only view. Every call here hits /api/admin/**, locked to ROLE_ADMIN. */
export default function AdminDashboard() {
  const [stats, setStats] = useState({});
  const [users, setUsers] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const reload = useCallback(async () => {
    const [statsData, usersData, requestsData] = await Promise.all([
      api.adminStats(),
      api.adminUsers(),
      api.adminRequests(),
    ]);
    setStats(statsData);
    setUsers(usersData);
    setRequests(requestsData);
  }, []);

  useEffect(() => {
    reload()
      .catch((error) => setMessage({ type: "error", text: error.message }))
      .finally(() => setLoading(false));
  }, [reload]);

  async function toggleActive(user) {
    try {
      if (user.active) {
        await api.deactivateUser(user.id);
      } else {
        await api.activateUser(user.id);
      }
      setMessage({
        type: "success",
        text: `${user.fullName} is now ${user.active ? "deactivated" : "active"}.`,
      });
      await reload();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    }
  }

  if (loading) {
    return <p className="empty">Loading admin data...</p>;
  }

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

      <section className="panel">
        <header className="panel__head">
          <span className="panel__tag">User management</span>
          <h2>
            All users <span className="count">{users.length}</span>
          </h2>
          <p>Deactivating an account blocks that person from logging in.</p>
        </header>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.fullName}</td>
                  <td className="muted">{user.email}</td>
                  <td>
                    <span className={`role role--${user.role.toLowerCase()}`}>{user.role}</span>
                  </td>
                  <td>{user.active ? "Active" : "Deactivated"}</td>
                  <td>
                    <button className="btn btn--ghost btn--sm" onClick={() => toggleActive(user)}>
                      {user.active ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <header className="panel__head">
          <span className="panel__tag">Oversight</span>
          <h2>
            All interview requests <span className="count">{requests.length}</span>
          </h2>
        </header>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Topic</th>
                <th>Student</th>
                <th>Mentor</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((request) => (
                <tr key={request.id}>
                  <td>{request.topic}</td>
                  <td className="muted">{request.student.fullName}</td>
                  <td className="muted">{request.mentor?.name ?? "—"}</td>
                  <td>
                    <StatusBadge status={request.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

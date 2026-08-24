import { useEffect, useState } from "react";
import { useAdminDashboard } from "../features/admin/useAdminDashboard";
import { useAdminProjects } from "../features/projects/useAdminProjects";
import ProjectAdminCard from "../features/projects/components/ProjectAdminCard";
import ProjectEditor from "../features/projects/components/ProjectEditor";
import AccessReviewCard from "../features/projects/components/AccessReviewCard";
import StatusBadge from "../components/StatusBadge";
import RequestCard from "../features/sessions/components/RequestCard";
import AssignMentorForm from "../features/mentors/components/AssignMentorForm";
import MentorProfileCard from "../features/mentors/components/MentorProfileCard";
import PaymentReviewCard from "../features/payments/components/PaymentReviewCard";
import PlanAdminCard from "../features/plans/components/PlanAdminCard";
import PlanEditor from "../features/plans/components/PlanEditor";
import EnrollmentReviewCard from "../features/plans/components/EnrollmentReviewCard";
import MaterialSendForm from "../features/materials/components/MaterialSendForm";
import { useSectionNav } from "../layout/SectionNav";

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
  activePlans: "Plans",
  planPaymentsToCheck: "Plan pay",
  activeEnrollments: "Enrolled",
  materials: "Material",
  liveProjects: "Projects",
  projectAccessToCheck: "Proj pay",
  contributors: "Contribs",
  awaitingRepoInvite: "To invite",
};

/** Everything an admin does: verify mentors, and match students to mentors. */
export default function AdminDashboard() {
  const { active: tab, register } = useSectionNav();
  // One hook for this screen - the facade layer. An admin genuinely needs users,
  // mentor profiles, sessions, two payment queues, plans and material at once, so
  // something has to compose them; better there, once, than sixteen useState here.
  const {
    stats, profiles, unassigned, mentors, users, requests,
    payments, plans, planPayments, materials,
    loading, message,
    verifyPayment, rejectPayment: doRejectPayment,
    approveMentor, rejectMentor: doRejectMentor,
    assignMentor, toggleUser,
    savePlanPrice, savePlan: doSavePlan, togglePlan,
    activateEnrollment, rejectEnrollment: doRejectEnrollment,
    uploadMaterial, shareMaterialLink, toggleMaterial,
  } = useAdminDashboard();

  // Its own hook: live projects are a separate feature with their own queues, and
  // a failure loading them should not blank the payments screen.
  const projectsAdmin = useAdminProjects();
  const [editingProject, setEditingProject] = useState(null);
  const [contributorsOf, setContributorsOf] = useState(null);

  const [mentorFilter, setMentorFilter] = useState("PENDING");
  const [assigning, setAssigning] = useState(null);
  // null = closed, "new" = creating, a plan object = editing that one.
  const [editingPlan, setEditingPlan] = useState(null);

  // The prompts stay in the screen: asking the user a question is a UI concern,
  // and a hook that popped a window.prompt() could not be used headlessly.
  async function reject(profile) {
    const reason = window.prompt(`Why are you rejecting ${profile.fullName}?`);
    if (reason) await doRejectMentor(profile, reason);
  }

  async function rejectPayment(payment) {
    const reason = window.prompt(`Why are you rejecting this payment from ${payment.studentName}?`);
    if (reason) await doRejectPayment(payment, reason);
  }

  async function rejectEnrollment(enrollment) {
    const reason = window.prompt(
      `Why are you rejecting ${enrollment.studentName}'s payment for ${enrollment.planName}?`
    );
    if (reason) await doRejectEnrollment(enrollment, reason);
  }

  async function assign(requestId, payload) {
    await assignMentor(requestId, payload);
    setAssigning(null);
  }

  async function saveProject(payload) {
    await projectsAdmin.saveProject(editingProject, payload);
    setEditingProject(null);
  }

  async function rejectAccess(access) {
    const reason = window.prompt(
      `Why are you rejecting ${access.studentName}'s payment for ${access.projectName}?`);
    if (reason) await projectsAdmin.reject(access, reason);
  }

  async function revokeAccess(access) {
    const reason = window.prompt(
      `Why are you revoking @${access.githubUsername}'s access to ${access.projectName}? `
      + "They see this reason.");
    if (reason) await projectsAdmin.revoke(access, reason);
  }

  async function showContributors(project) {
    try {
      setContributorsOf({ project, rows: await projectsAdmin.contributors(project.id) });
    } catch (e) {
      setMessage({ type: "error", text: e.message });
    }
  }

  async function savePlan(payload) {
    await doSavePlan(editingPlan, payload);
    setEditingPlan(null);
  }

  const pendingCount = profiles.filter((p) => p.verificationStatus === "PENDING").length;

  // Registered before the early return below - a hook after a conditional
  // return would change hook order between renders and React would throw.
  useEffect(() => {
    register(
      [
        { key: "payments", label: "Payments", icon: "💳", count: payments.length, alert: true },
        { key: "planpay", label: "Plan payments", icon: "🧾", count: planPayments.length, alert: true },
        { key: "requests", label: "Assign", icon: "🔗", count: unassigned.length, alert: true },
        { key: "verify", label: "Mentors", icon: "🧭", count: pendingCount, alert: true },
        { key: "plans", label: "Plans & prices", icon: "🎯", count: plans.length },
        { key: "material", label: "Study material", icon: "📚", count: materials.length },
        { key: "projaccess", label: "Project access", icon: "🔑",
          count: projectsAdmin.pending.length + projectsAdmin.awaitingInvite.length
                 + projectsAdmin.pastExpiry.length, alert: true },
        { key: "projects", label: "Live projects", icon: "🛠", count: projectsAdmin.projects.length },
        { key: "users", label: "Users", icon: "👥" },
        { key: "all", label: "All requests", icon: "🗂" },
      ],
      "payments"
    );
  }, [register, payments.length, unassigned.length, pendingCount, planPayments.length,
      plans.length, materials.length, projectsAdmin.pending.length,
      projectsAdmin.awaitingInvite.length, projectsAdmin.pastExpiry.length,
      projectsAdmin.projects.length]);

  if (loading) return <p className="empty">Loading admin data...</p>;

  const shown = profiles.filter((p) => p.verificationStatus === mentorFilter);

  const MENTOR_FILTERS = [
    { key: "PENDING", label: "Awaiting review" },
    { key: "APPROVED", label: "Verified" },
    { key: "REJECTED", label: "Rejected" },
    { key: "INCOMPLETE", label: "Not submitted" },
  ];

  const students = users.filter((u) => u.role === "STUDENT" && u.active);

  return (
    <div className="admin">
      {editingPlan && (
        <PlanEditor
          plan={editingPlan === "new" ? null : editingPlan}
          onSave={savePlan}
          onCancel={() => setEditingPlan(null)}
        />
      )}

      {editingProject && (
        <ProjectEditor
          project={editingProject === "new" ? null : editingProject}
          reviewers={users.filter((u) => u.role !== "STUDENT" && u.active)}
          onSave={saveProject}
          onCancel={() => setEditingProject(null)}
        />
      )}

      {message && <p className={`notice notice--${message.type}`}>{message.text}</p>}
      {projectsAdmin.message && (
        <p className={`notice notice--${projectsAdmin.message.type}`}>
          {projectsAdmin.message.text}
        </p>
      )}

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
                    <button className="btn btn--primary" onClick={() => approveMentor(p)}>Approve</button>
                    <button className="btn btn--ghost" onClick={() => reject(p)}>Reject</button>
                  </div>
                )}
              </MentorProfileCard>
            ))}
          </div>
        </section>
      )}

      {tab === "planpay" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Money in</span>
            <h2>
              Plan payments to verify <span className="count">{planPayments.length}</span>
            </h2>
            <p>
              Students who have paid for a plan. Check the UTR against your bank, then
              confirm — that is what starts their access and unlocks anything shared
              with that plan&apos;s members.
            </p>
          </header>

          {planPayments.length === 0 && (
            <p className="empty">No plan payments waiting to be checked.</p>
          )}

          <div className="card-list">
            {planPayments.map((e) => (
              <EnrollmentReviewCard
                key={e.id}
                enrollment={e}
                onActivate={activateEnrollment}
                onReject={rejectEnrollment}
              />
            ))}
          </div>
        </section>
      )}

      {tab === "plans" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Price list</span>
            <h2>
              Plans &amp; prices <span className="count">{plans.length}</span>
            </h2>
            <p>
              Change a price and students see the new number on their next page load —
              no deploy, no restart. Anyone who already bought a plan keeps the price
              they paid.
            </p>
          </header>

          <div className="chips-row">
            <button className="btn btn--primary btn--sm" onClick={() => setEditingPlan("new")}>
              + New plan
            </button>
          </div>

          {plans.length === 0 && <p className="empty">No plans yet. Create the first one.</p>}

          <div className="card-list">
            {plans.map((plan) => (
              <PlanAdminCard
                key={plan.id}
                plan={plan}
                onSavePrice={savePlanPrice}
                onToggleActive={togglePlan}
                onEdit={setEditingPlan}
              />
            ))}
          </div>
        </section>
      )}

      {tab === "material" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Send to students</span>
            <h2>
              Study material <span className="count">{materials.length}</span>
            </h2>
            <p>
              Upload a file or share a link, and choose who gets it: every student, one
              student, or the members of a plan. Students only ever see what was
              addressed to them.
            </p>
          </header>

          <MaterialSendForm
            students={students}
            plans={plans}
            onUpload={uploadMaterial}
            onShareLink={shareMaterialLink}
          />

          <h3 className="subhead">Already sent</h3>

          {materials.length === 0 && <p className="empty">Nothing sent yet.</p>}

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Title</th><th>Kind</th><th>Who sees it</th><th>Sent</th><th>Status</th><th />
                </tr>
              </thead>
              <tbody>
                {materials.map((m) => (
                  <tr key={m.id}>
                    <td>
                      {m.title}
                      {m.fileName && <div className="muted mono small">{m.fileName}</div>}
                    </td>
                    <td className="muted">{m.kind}</td>
                    <td>
                      <span className="material__chip">{m.audienceLabel}</span>
                    </td>
                    <td className="muted">{new Date(m.createdAt).toLocaleDateString()}</td>
                    <td>
                      <span className={`badge ${m.active ? "badge--completed" : "badge--cancelled"}`}>
                        {m.active ? "LIVE" : "HIDDEN"}
                      </span>
                    </td>
                    <td>
                      <button className="btn btn--ghost btn--sm" onClick={() => toggleMaterial(m)}>
                        {m.active ? "Hide" : "Publish"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === "projaccess" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">Repository access</span>
            <h2>Project access</h2>
            <p>
              Three queues, and they are not the same job. Money to check, people
              to add on GitHub, and people whose access has run out.
            </p>
          </header>

          {/* Deliberately first: these people have paid and still cannot open the
              repo. It is the only queue where somebody is actively worse off. */}
          <h3 className="subhead">
            Add on GitHub <span className="count">{projectsAdmin.awaitingInvite.length}</span>
          </h3>
          {projectsAdmin.awaitingInvite.length === 0 ? (
            <p className="empty">Nobody is waiting to be added.</p>
          ) : (
            <>
              <p className="notice notice--error">
                These contributors have paid and their access is active in here, but
                they cannot open the repository until you add them on GitHub.
              </p>
              <div className="card-list">
                {projectsAdmin.awaitingInvite.map((a) => (
                  <AccessReviewCard key={a.id} access={a} mode="invite"
                    onConfirmInvite={projectsAdmin.confirmInvite} onRevoke={revokeAccess} />
                ))}
              </div>
            </>
          )}

          <h3 className="subhead">
            Payments to verify <span className="count">{projectsAdmin.pending.length}</span>
          </h3>
          {projectsAdmin.pending.length === 0 ? (
            <p className="empty">No access payments waiting.</p>
          ) : (
            <div className="card-list">
              {projectsAdmin.pending.map((a) => (
                <AccessReviewCard key={a.id} access={a} mode="payment"
                  onApprove={projectsAdmin.approve} onReject={rejectAccess} />
              ))}
            </div>
          )}

          <h3 className="subhead">
            Expired but still on the repo{" "}
            <span className="count">{projectsAdmin.pastExpiry.length}</span>
          </h3>
          {projectsAdmin.pastExpiry.length === 0 ? (
            <p className="empty">Nothing has outlived its access window.</p>
          ) : (
            <>
              <p className="pay-note">
                Nothing removes collaborators automatically, so these people still
                have push access after their window closed.
              </p>
              <div className="card-list">
                {projectsAdmin.pastExpiry.map((a) => (
                  <AccessReviewCard key={a.id} access={a} mode="expired"
                    onRevoke={revokeAccess} />
                ))}
              </div>
            </>
          )}
        </section>
      )}

      {tab === "projects" && (
        <section className="panel">
          <header className="panel__head">
            <span className="panel__tag">The catalogue</span>
            <h2>
              Live projects <span className="count">{projectsAdmin.projects.length}</span>
            </h2>
            <p>
              Our own private repos, sold as contributor access. Set the price and
              the seat limit — one reviewer can only review so many newcomers at once.
            </p>
          </header>

          <div className="chips-row">
            <button className="btn btn--primary btn--sm" onClick={() => setEditingProject("new")}>
              + New project
            </button>
          </div>

          {projectsAdmin.projects.length === 0 && (
            <p className="empty">No projects yet. Add the first one.</p>
          )}

          <div className="card-list">
            {projectsAdmin.projects.map((project) => (
              <ProjectAdminCard
                key={project.id}
                project={project}
                onSavePrice={projectsAdmin.savePrice}
                onToggleActive={projectsAdmin.toggleProject}
                onEdit={setEditingProject}
                onViewContributors={showContributors}
              />
            ))}
          </div>

          {contributorsOf && (
            <>
              <h3 className="subhead">
                Contributors on {contributorsOf.project.name}{" "}
                <span className="count">{contributorsOf.rows.length}</span>
                <button className="linkish" onClick={() => setContributorsOf(null)}>
                  hide
                </button>
              </h3>
              {contributorsOf.rows.length === 0 ? (
                <p className="empty">Nobody has access to this project yet.</p>
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Student</th><th>GitHub</th><th>Granted</th><th>Expires</th>
                        <th>On repo?</th><th />
                      </tr>
                    </thead>
                    <tbody>
                      {contributorsOf.rows.map((r) => (
                        <tr key={r.id}>
                          <td>{r.studentName}<div className="muted small">{r.studentEmail}</div></td>
                          <td className="mono">@{r.githubUsername}</td>
                          <td className="muted">
                            {r.grantedAt ? new Date(r.grantedAt).toLocaleDateString() : "—"}
                          </td>
                          <td className="muted">
                            {r.expiresAt ? new Date(r.expiresAt).toLocaleDateString() : "—"}
                          </td>
                          <td>
                            <span className={`badge ${
                              r.collaboratorGranted ? "badge--completed" : "badge--pending"}`}>
                              {r.collaboratorGranted ? "YES" : "NOT YET"}
                            </span>
                          </td>
                          <td>
                            <button className="btn btn--ghost btn--sm"
                                    onClick={() => revokeAccess(r)}>
                              Revoke
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
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
                      <button className="btn btn--ghost btn--sm" onClick={() => toggleUser(u)}>
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

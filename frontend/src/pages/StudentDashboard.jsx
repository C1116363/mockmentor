import { useEffect, useState } from "react";
import Notice from "../components/Notice";
import { useSessions } from "../features/sessions/useSessions";
import { usePlans } from "../features/plans/usePlans";
import { useMaterials } from "../features/materials/useMaterials";
import { useProjects } from "../features/projects/useProjects";
import ProjectCard from "../features/projects/components/ProjectCard";
import ProjectRequestModal from "../features/projects/components/ProjectRequestModal";
import ProjectPayModal from "../features/projects/components/ProjectPayModal";
import { LIVE_STATUSES } from "../features/sessions/sessionRules";
import RequestCard from "../features/sessions/components/RequestCard";
import SlotPicker from "../features/sessions/components/SlotPicker";
import UpcomingInterviews from "../features/sessions/components/UpcomingInterviews";
import PayModal from "../features/payments/components/PayModal";
import PlanCard from "../features/plans/components/PlanCard";
import PlanPayModal from "../features/plans/components/PlanPayModal";
import MaterialCard from "../features/materials/components/MaterialCard";
import ConfirmDialog from "../components/ConfirmDialog";
import Skeleton from "../components/Skeleton";
import { useSectionNav } from "../layout/SectionNav";

const EXPERIENCE_LEVELS = ["Fresher", "0-1 years", "1-3 years", "3-5 years", "5+ years"];

/**
 * The two kinds of hour you can book.
 *
 * Same slot, same price, same mentor pool - what differs is what you get at the
 * end, so the copy on the form changes with it rather than staying generic and
 * leaving the student to guess which one they want.
 */
const SESSION_TYPES = [
  {
    key: "MOCK_INTERVIEW",
    label: "Mock interview",
    sub: "Interviewed under real pressure",
    icon: "🎙️",
    heading: "What do you want to practise?",
    lede: "Tell us the round you're preparing for, then pick an hour that suits you.",
    topicLabel: "Topic",
    topicHint: "Spring Boot backend round",
    notesLabel: "Anything the interviewer should know? (optional)",
    notesHint: "Final year student, weak on JPA relationships.",
    outcome: "You'll get a written scorecard afterwards — ratings per skill and what to fix.",
    submit: "Book interview & pay",
  },
  {
    key: "MENTORING",
    label: "Mentoring session",
    sub: "Just talk it through with an expert",
    icon: "💬",
    heading: "What do you want to talk about?",
    lede: "Not an interview — an hour to discuss whatever you're stuck on with a senior engineer.",
    topicLabel: "What do you need help with?",
    topicHint: "Which stack should I specialise in?",
    notesLabel: "Give them some context (optional)",
    notesHint: "Two offers on the table and I can't decide. Happy to share both.",
    outcome: "No ratings and no scorecard — you'll get written notes from the discussion.",
    submit: "Book session & pay",
  },
];

const EMPTY_FORM = {
  sessionType: "MOCK_INTERVIEW",
  topic: "",
  experienceLevel: "Fresher",
  preferredSlot: "",
  notes: "",
};

export default function StudentDashboard() {
  const { active: tab, register, go: setTab } = useSectionNav();

  // Three hooks, one per feature - this is the facade layer. Each loads and
  // fails on its own, so a broken plans call cannot blank out the interview list.
  const { live, done, unpaidCount, upcoming, loading, book, cancel, attachCv } =
    useSessions();
  const plansFeature = usePlans();
  const { materials } = useMaterials();
  const projectsFeature = useProjects();

  const [form, setForm] = useState(EMPTY_FORM);
  const [date, setDate] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [payingFor, setPayingFor] = useState(null);
  // Held outside the form object: a File is not JSON, and it goes up in its own
  // call after the booking exists.
  const [cv, setCv] = useState(null);
  const [gettingPlan, setGettingPlan] = useState(null);
  const [payingPlan, setPayingPlan] = useState(null);
  // requesting = the project whose request form is open; payingProject = the
  // access row being paid for. Two states because they are two steps.
  const [requesting, setRequesting] = useState(null);
  const [payingProject, setPayingProject] = useState(null);
  // The booking whose cancel dialog is open.
  const [cancelling, setCancelling] = useState(null);

  async function getPlan(plan) {
    setGettingPlan(plan.id);
    setMessage(null);
    try {
      // Straight into the payment modal - choosing a plan and paying for it is
      // one intention, and a student who has to hunt for a second button leaves.
      setPayingPlan(await plansFeature.enroll(plan.id));
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    } finally {
      setGettingPlan(null);
    }
  }

  async function requestProjectAccess(payload) {
    const access = await projectsFeature.requestAccess(requesting.id, payload);
    setRequesting(null);
    // Straight into paying - requesting and paying are one intention, and a
    // student who has to find a second button usually just leaves.
    setPayingProject(access);
  }

  async function afterProjectPayment() {
    setPayingProject(null);
    setMessage({
      type: "success",
      text: "Thanks! Once an admin confirms the payment you'll get a collaborator "
        + "invite from GitHub by email.",
    });
    await projectsFeature.reload();
  }

  async function afterPlanPayment() {
    setPayingPlan(null);
    setMessage({
      type: "success",
      text: "Thanks! We're checking your payment. Your plan unlocks as soon as an admin confirms it.",
    });
    await plansFeature.reload();
  }

  const updateField = (e) => setForm((c) => ({ ...c, [e.target.name]: e.target.value }));

  const session = SESSION_TYPES.find((t) => t.key === form.sessionType) ?? SESSION_TYPES[0];

  /**
   * Switching kind clears the topic and notes.
   *
   * "Spring Boot backend round" is a sensible interview topic and a strange
   * thing to want to discuss. Carrying it over would leave a half-wrong booking
   * that reads as if it were deliberate.
   */
  function chooseSessionType(key) {
    if (key === form.sessionType) return;
    setForm((c) => ({ ...c, sessionType: key, topic: "", notes: "" }));
    setFieldErrors({});
  }

  async function submitRequest(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setFieldErrors({});

    if (!form.preferredSlot) {
      setSubmitting(false);
      setFieldErrors({ preferredSlot: "Pick a slot" });
      return;
    }

    try {
      const created = await book(form);

      /*
       * The CV goes up after the booking exists, and a failure here must not
       * lose the booking - the slot is the scarce thing, the file is not. So it
       * is caught separately and reported as a note rather than as a failure.
       */
      let cvWarning = null;
      if (cv) {
        try {
          await attachCv(created.id, cv);
        } catch (e) {
          cvWarning = `Booked, but your CV didn't upload: ${e.message} `
            + "You can add it from the booking card.";
        }
      }

      setForm(EMPTY_FORM);
      setDate("");
      setCv(null);
      if (cvWarning) setMessage({ type: "error", text: cvWarning });
      setPayingFor(created);
    } catch (error) {
      setFieldErrors(error.fieldErrors);
      setMessage({ type: "error", text: error.message });
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmCancel() {
    // Throwing leaves the dialog open with the reason shown, rather than closing
    // it and dropping the error into a banner they may not look at.
    await cancel(cancelling.id);
    setCancelling(null);
    setMessage({ type: "success", text: "Booking cancelled. The slot is free again." });
  }

  async function afterPayment() {
    setPayingFor(null);
    setMessage({
      type: "success",
      text: "Thanks! We're checking your payment. Your slot is held in the meantime.",
    });
    setTab("active");
  }


  // Re-registering with the same values is a no-op, so this is safe to run
  // whenever the counts change.

  useEffect(() => {
    register(
      [
        { key: "book", label: "Book an interview", icon: "＋" },
        { key: "active", label: "My interviews", icon: "📅", count: live.length, alert: unpaidCount > 0 },
        { key: "plans", label: "Plans", icon: "🎯", count: plansFeature.activeCount, alert: plansFeature.needsPaymentCount > 0 },
        { key: "material", label: "Study material", icon: "📚", count: materials.length },
        { key: "projects", label: "Live projects", icon: "🛠", count: projectsFeature.activeCount,
          alert: projectsFeature.needsPaymentCount > 0 },
        { key: "history", label: "History", icon: "🗂", count: done.length },
      ],
      "book"
    );
  }, [register, live.length, done.length, unpaidCount, plansFeature.activeCount, plansFeature.needsPaymentCount, materials.length,
      projectsFeature.activeCount, projectsFeature.needsPaymentCount]);

  const actions = (request) => (
    <>
      {request.status === "AWAITING_PAYMENT" && (
        <button className="btn btn--primary" onClick={() => setPayingFor(request)}>
          Pay now
        </button>
      )}
      {LIVE_STATUSES.includes(request.status) && (
        <button className="btn btn--ghost" onClick={() => setCancelling(request)}>
          Cancel request
        </button>
      )}
    </>
  );

  return (
    <>
      {payingFor && (
        <PayModal
          request={payingFor}
          onDone={afterPayment}
          onClose={() => {
            setPayingFor(null);
            setTab("active");
            setMessage({
              type: "error",
              text: "Your slot is held but not confirmed. Use “Pay now” to finish.",
            });
          }}
        />
      )}

      {payingPlan && (
        <PlanPayModal
          enrollment={payingPlan}
          onDone={afterPlanPayment}
          onClose={() => {
            setPayingPlan(null);
            setTab("plans");
            setMessage({
              type: "error",
              text: "Your plan isn't active yet. Use “Finish payment” on the card when you're ready.",
            });
          }}
        />
      )}

      {cancelling && (
        <ConfirmDialog
          title="Cancel this booking?"
          confirmLabel="Cancel booking"
          cancelLabel="Keep it"
          onCancel={() => setCancelling(null)}
          onConfirm={confirmCancel}
        >
          <p>
            <strong>{cancelling.topic}</strong>
            <br />
            {cancelling.preferredSlot
              && new Date(cancelling.preferredSlot).toLocaleString(undefined,
                   { dateStyle: "full", timeStyle: "short" })}
          </p>
          <p>
            The hour goes back to whoever offered it, so somebody else can book
            it. You would need to book again from scratch.
          </p>
          {cancelling.status === "AWAITING_PAYMENT" && (
            <p className="confirm__warn">
              You have not paid for this one, so there is nothing to refund.
            </p>
          )}
          {cancelling.status !== "AWAITING_PAYMENT" && (
            <p className="confirm__warn">
              You have already paid for this. Cancelling does not refund you
              automatically — contact an admin.
            </p>
          )}
        </ConfirmDialog>
      )}

      {requesting && (
        <ProjectRequestModal
          project={requesting}
          onSubmit={requestProjectAccess}
          onClose={() => setRequesting(null)}
        />
      )}

      {payingProject && (
        <ProjectPayModal
          access={payingProject}
          onDone={afterProjectPayment}
          onClose={() => {
            setPayingProject(null);
            setTab("projects");
            setMessage({
              type: "error",
              text: "Your request isn't paid for yet. Use \u201cFinish payment\u201d on the card.",
            });
          }}
        />
      )}

      <UpcomingInterviews
        interviews={upcoming}
        otherPartyLabel="Interviewer"
        otherParty={(r) => r.mentor?.name ?? "—"}
      />

      {message && (
        <Notice tone={message.type} onDismiss={() => setMessage(null)}>
          {message.text}
        </Notice>
      )}

      {tab === "book" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">New booking</span>
            <h2>{session.heading}</h2>
            <p>{session.lede}</p>
          </header>

          <div className="session-pick">
            {SESSION_TYPES.map((t) => (
              <button
                key={t.key}
                type="button"
                className={`session-pick__opt ${
                  form.sessionType === t.key ? "session-pick__opt--on" : ""
                }`}
                onClick={() => chooseSessionType(t.key)}
                aria-pressed={form.sessionType === t.key}
              >
                <span className="session-pick__icon">{t.icon}</span>
                <strong>{t.label}</strong>
                <small>{t.sub}</small>
              </button>
            ))}
          </div>

          <p className="session-pick__outcome">{session.outcome}</p>

          <form className="form" onSubmit={submitRequest}>
            <label className="field">
              <span>{session.topicLabel}</span>
              <input
                name="topic"
                value={form.topic}
                onChange={updateField}
                placeholder={session.topicHint}
                required
              />
              {fieldErrors.topic && <small className="field__error">{fieldErrors.topic}</small>}
            </label>

            <label className="field">
              <span>Your experience</span>
              <select name="experienceLevel" value={form.experienceLevel} onChange={updateField}>
                {EXPERIENCE_LEVELS.map((level) => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </select>
            </label>

            <SlotPicker
              date={date}
              onDateChange={setDate}
              value={form.preferredSlot}
              onChange={(slot) => setForm((c) => ({ ...c, preferredSlot: slot }))}
              error={fieldErrors.preferredSlot}
              sessionType={form.sessionType}
            />

            <label className="field">
              <span>{session.notesLabel}</span>
              <textarea
                name="notes"
                value={form.notes}
                onChange={updateField}
                rows={3}
                placeholder={session.notesHint}
              />
            </label>

            <label className="field">
              <span>
                Your CV <em>(optional)</em>
              </span>
              <input
                type="file"
                accept=".pdf,.doc,.docx,application/pdf"
                onChange={(e) => setCv(e.target.files?.[0] ?? null)}
              />
              <small className="field__hint">
                {session.key === "MENTORING"
                  ? "Helps the mentor give advice that fits where you actually are."
                  : "Your interviewer reads it beforehand, so the questions are about your "
                    + "own projects rather than generic ones."}
                {" "}PDF or Word, up to 5 MB — PDF looks the same on their machine as it
                does on yours.
              </small>
              {cv && (
                <small className="field__hint cv-picked">
                  📎 {cv.name} · {(cv.size / 1024).toFixed(0)} KB
                </small>
              )}
            </label>

            <button
              className="btn btn--primary"
              type="submit"
              disabled={submitting || !form.preferredSlot}
            >
              {submitting ? (
                <>
                  <span className="spinner" /> Booking
                </>
              ) : (
                session.submit
              )}
            </button>
          </form>
        </section>
      )}

      {tab === "active" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">In progress</span>
            <h2>
              My interviews <span className="count">{live.length}</span>
            </h2>
            <p>Everything booked but not finished yet.</p>
          </header>

          {loading && <Skeleton rows={2} />}
          {!loading && live.length === 0 && (
            <div className="empty">
              <p>Nothing booked right now.</p>
              <button className="btn btn--primary" onClick={() => setTab("book")}>
                Book an interview
              </button>
            </div>
          )}

          <div className="card-list">
            {live.map((r) => (
              <RequestCard key={r.id} request={r}>
                {actions(r)}
              </RequestCard>
            ))}
          </div>
        </section>
      )}

      {tab === "plans" && (
        <section className="panel panel--student">
          <header className="panel__head">
            <span className="panel__tag">Learn with us</span>
            <h2>
              Plans <span className="count">{plansFeature.plans.length}</span>
            </h2>
            <p>
              Longer tracks with our experts — placement prep, or a technology taught
              properly. Pick one, pay by UPI, and it unlocks once an admin confirms.
            </p>
          </header>

          {plansFeature.plans.length === 0 && <p className="empty">No plans on sale right now.</p>}

          <div className="plan-grid">
            {plansFeature.plans.map((plan) => (
              <PlanCard
                key={plan.id}
                plan={plan}
                enrollment={plansFeature.enrollmentOf(plan.id)}
                onGet={getPlan}
                onPay={setPayingPlan}
                busy={gettingPlan === plan.id}
              />
            ))}
          </div>
        </section>
      )}

      {tab === "material" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">From our experts</span>
            <h2>
              Study material <span className="count">{materials.length}</span>
            </h2>
            <p>
              Notes, PDFs and links sent to you by an admin — some to every student,
              some just for you, some unlocked by a plan you hold.
            </p>
          </header>

          {materials.length === 0 && (
            <div className="empty">
              <p>Nothing shared with you yet.</p>
              <button className="btn btn--primary" onClick={() => setTab("plans")}>
                See the plans
              </button>
            </div>
          )}

          <div className="card-list">
            {materials.map((m) => (
              <MaterialCard key={m.id} material={m} />
            ))}
          </div>
        </section>
      )}

      {tab === "projects" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">Contribute for real</span>
            <h2>
              Live projects <span className="count">{projectsFeature.projects.length}</span>
            </h2>
            <p>
              These are our own private codebases, running in production — not open
              source. Pay for contributor access, raise pull requests, and a senior
              engineer reviews and merges them. That review is the part you
              can&apos;t get from a public repo nobody looks at.
            </p>
          </header>

          {projectsFeature.awaitingInviteCount > 0 && (
            <p className="notice notice--success">
              Your payment is confirmed. We&apos;re adding you to the repository —
              watch for a collaborator invite from GitHub by email.
            </p>
          )}

          {projectsFeature.projects.length === 0 && (
            <p className="empty">No projects open for contributors right now.</p>
          )}

          <div className="project-grid">
            {projectsFeature.projects.map((project) => (
              <ProjectCard
                key={project.id}
                project={project}
                access={projectsFeature.accessOf(project.id)}
                onRequest={setRequesting}
                onPay={setPayingProject}
              />
            ))}
          </div>
        </section>
      )}

      {tab === "history" && (
        <section className="panel panel--mentor">
          <header className="panel__head">
            <span className="panel__tag">Past</span>
            <h2>
              History <span className="count">{done.length}</span>
            </h2>
            <p>Completed interviews and their scorecards, plus anything cancelled.</p>
          </header>

          {done.length === 0 && <p className="empty">No finished interviews yet.</p>}

          <div className="card-list">
            {done.map((r) => (
              <RequestCard key={r.id} request={r} />
            ))}
          </div>
        </section>
      )}
    </>
  );
}

import { DIFFICULTY_CLASS, daysLeftLabel, projectCardState } from "../projectRules";
import { formatPrice } from "../../../utils/format";

/**
 * One live project on the catalogue.
 *
 * The repository link only appears when `access.currentlyActive` - the server
 * sends null for `repoUrl` otherwise, so there is nothing to link to and nothing
 * to leak. That is worth knowing when reading this: the absence of a link is the
 * access control working, not a rendering bug.
 */
export default function ProjectCard({ project, access, onRequest, onPay, busy }) {
  const state = projectCardState(access, project);

  return (
    <article className={`project ${state === "GRANTED" ? "project--mine" : ""}`}>
      <header className="project__head">
        <div>
          <h3 className="project__name">{project.name}</h3>
          {project.summary && <p className="project__summary">{project.summary}</p>}
        </div>
        <span className={`diff diff--${DIFFICULTY_CLASS[project.difficulty]}`}>
          {project.difficultyLabel}
        </span>
      </header>

      {project.techStack.length > 0 && (
        <ul className="project__stack">
          {project.techStack.map((tech) => (
            <li key={tech}>{tech}</li>
          ))}
        </ul>
      )}

      {project.description && <p className="project__desc">{project.description}</p>}

      {project.sampleTasks.length > 0 && (
        <div className="project__tasks">
          <h4>What you could pick up</h4>
          <ul>
            {project.sampleTasks.map((task) => (
              <li key={task}>{task}</li>
            ))}
          </ul>
        </div>
      )}

      <dl className="project__facts">
        <div>
          <dt>Access</dt>
          <dd>{project.accessDurationDays} days</dd>
        </div>
        {project.leadReviewer && (
          <div>
            <dt>Reviews your PRs</dt>
            <dd>{project.leadReviewer}</dd>
          </div>
        )}
        <div>
          <dt>Contributors</dt>
          <dd>
            {project.seatsTaken}
            {project.maxContributors ? ` / ${project.maxContributors}` : ""}
          </dd>
        </div>
      </dl>

      <p className="project__price">
        <span className="project__rupee">₹</span>
        <span className="project__amount">{formatPrice(project.price)}</span>
      </p>

      <div className="project__foot">
        {state === "GRANTED" && (
          <>
            <span className="badge badge--completed">You&apos;re a contributor</span>
            <a className="btn btn--primary btn--wide" href={project.repoUrl}
               target="_blank" rel="noopener noreferrer">
              Open the repository →
            </a>
            {project.onboardingUrl && (
              <a className="linkish" href={project.onboardingUrl}
                 target="_blank" rel="noopener noreferrer">
                Read this first
              </a>
            )}
            <small className="project__note">
              {daysLeftLabel(access.expiresAt)} · raise a pull request and{" "}
              {project.leadReviewer ?? "a senior engineer"} will review it.
            </small>
          </>
        )}

        {state === "INVITE_PENDING" && (
          <>
            <span className="badge badge--pending">Being added to the repo</span>
            <small className="project__note">
              Your payment is confirmed. We&apos;re adding <strong>@{access.githubUsername}</strong>{" "}
              as a collaborator — you&apos;ll get an invite from GitHub by email.
            </small>
          </>
        )}

        {state === "CHECKING" && (
          <>
            <span className="badge badge--pending">Checking payment</span>
            <small className="project__note">
              An admin is confirming your UPI reference. You&apos;ll be added to the
              repository once they do.
            </small>
          </>
        )}

        {state === "UNPAID" && (
          <button className="btn btn--primary btn--wide" onClick={() => onPay(access)}>
            Finish payment
          </button>
        )}

        {state === "REJECTED" && (
          <>
            <span className="badge badge--rejected">Payment rejected</span>
            {access.rejectionReason && (
              <small className="project__note">{access.rejectionReason}</small>
            )}
            <button className="btn btn--primary btn--wide" onClick={() => onPay(access)}>
              Send new proof
            </button>
          </>
        )}

        {state === "FULL" && (
          <>
            <span className="badge badge--cancelled">Full</span>
            <small className="project__note">
              All {project.maxContributors} contributor seats are taken. One senior
              engineer can only review so many people at once — seats free up as
              access expires.
            </small>
          </>
        )}

        {state === "REQUEST" && (
          <button className="btn btn--primary btn--wide" onClick={() => onRequest(project)}
                  disabled={busy}>
            {busy ? "Starting..." : "Request access"}
          </button>
        )}
      </div>
    </article>
  );
}

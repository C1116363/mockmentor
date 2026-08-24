package com.learn.interviewmentor.github;

/**
 * Adds and removes collaborators on one of our private repositories.
 *
 * Behind an interface for the same reason {@code MeetingLinkGenerator} is: how
 * access gets granted is a deployment decision, not a business rule. The service
 * layer knows only "grant this person access to this repo" - not whether that
 * happens through the GitHub API or a human clicking **Add people**.
 *
 * <h2>Both implementations must be honest about what happened</h2>
 * The one thing this interface exists to protect: a grant that did not happen
 * must never look like one that did. A student who has paid and cannot see the
 * code yet is a support ticket. A student who has paid and is *told* they have
 * access but does not is a lost customer. Hence {@link GrantResult} rather than
 * {@code void} - the caller records the difference.
 */
public interface CollaboratorGranter {

    /** Contributors get "push"; "pull" would be read-only. */
    String PUSH = "push";

    /**
     * @param repoFullName   "owner/repo"
     * @param githubUsername the account to add
     * @param permission     {@link #PUSH} for a contributor
     */
    GrantResult grant(String repoFullName, String githubUsername, String permission);

    /** Called when access expires or is revoked. */
    GrantResult revoke(String repoFullName, String githubUsername);

    /** Human-readable, for the admin screen: "GitHub API" or "Manual". */
    String describe();

    /**
     * What happened.
     *
     * @param done    true only if the person now genuinely has - or has genuinely
     *                lost - access. False means somebody still has to act.
     * @param message what to tell the admin: the next action, or the error.
     */
    record GrantResult(boolean done, String message) {

        public static GrantResult done(String message) {
            return new GrantResult(true, message);
        }

        /** Nothing is broken; a human just has to finish it. */
        public static GrantResult needsAction(String message) {
            return new GrantResult(false, message);
        }

        /** Something went wrong and it needs looking at. */
        public static GrantResult failed(String message) {
            return new GrantResult(false, message);
        }
    }
}

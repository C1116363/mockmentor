package com.learn.interviewmentor.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The default: we record who should have access, and a human does the clicking.
 *
 * This is not a placeholder for the API version - it is a legitimate choice, and
 * for a small team the better one. Push access to a private repo is worth a
 * person looking at, and it avoids keeping a token with repo-admin scope in the
 * application's environment, where a compromise of the app becomes a compromise
 * of every repository.
 *
 * What it does guarantee is that the pending work cannot be lost: the access row
 * sits ACTIVE with {@code collaboratorGranted = false}, which surfaces on the
 * admin screen as a queue carrying the exact repo URL and username to act on.
 */
@Component
@ConditionalOnProperty(name = "app.github.provider", havingValue = "manual", matchIfMissing = true)
public class ManualCollaboratorGranter implements CollaboratorGranter {

    private static final Logger log = LoggerFactory.getLogger(ManualCollaboratorGranter.class);

    @Override
    public GrantResult grant(String repoFullName, String githubUsername, String permission) {
        // Logged at info so there is a record outside the database of every
        // access that was approved, and when.
        log.info("ACTION NEEDED: add '{}' to {} with '{}' permission -> {}",
                githubUsername, repoFullName, permission, accessSettingsUrl(repoFullName));

        return GrantResult.needsAction(
                "Add @" + githubUsername + " to " + repoFullName + " as a collaborator: "
                        + accessSettingsUrl(repoFullName));
    }

    @Override
    public GrantResult revoke(String repoFullName, String githubUsername) {
        log.info("ACTION NEEDED: remove '{}' from {} -> {}",
                githubUsername, repoFullName, accessSettingsUrl(repoFullName));

        return GrantResult.needsAction(
                "Remove @" + githubUsername + " from " + repoFullName + ": "
                        + accessSettingsUrl(repoFullName));
    }

    @Override
    public String describe() {
        return "Manual - an admin adds the collaborator on GitHub";
    }

    /** Deep-links straight to the page the admin needs, not the repo home. */
    private static String accessSettingsUrl(String repoFullName) {
        return "https://github.com/" + repoFullName + "/settings/access";
    }
}

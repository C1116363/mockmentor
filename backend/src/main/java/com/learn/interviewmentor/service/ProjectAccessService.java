package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.project.ProjectAccessApplicationDto;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.LiveProjectService;
import com.learn.interviewmentor.service.ProjectAccessService;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.project.ProjectAccessVo;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contributor access to a private repository: requested, paid for, granted.
 *
 * Approving one of these has an effect outside our database - a real GitHub
 * account gains push access to a real private repo. That is why the payment and
 * the invite are tracked as two separate things.
 */
public interface ProjectAccessService {

    ProjectAccessVo apply(Long projectId, ProjectAccessApplicationDto dto, User student);

    List<ProjectAccessVo> mine(User student);

    /** Project ids this student currently holds. Drives repo visibility in the catalogue. */
    List<Long> activeProjectIds(User student);

    PaymentInstructionsVo instructionsFor(Long accessId, User caller);

    ProjectAccessVo submitProof(Long accessId, String upiReference, MultipartFile screenshot, User student);

    ProjectAccessVo cancel(Long accessId, User student);

    /** Fix a mistyped handle before it has been granted. */
    ProjectAccessVo changeGithubUsername(Long accessId, String githubUsername, User student);

    List<ProjectAccessVo> awaitingReview();

    /** Paid and approved, but nobody has added them on GitHub yet. */
    List<ProjectAccessVo> awaitingCollaboratorInvite();

    /** Still ACTIVE but past expiry - these people should be off the repo. */
    List<ProjectAccessVo> pastExpiry();

    List<ProjectAccessVo> all();

    List<ProjectAccessVo> contributorsOn(Long projectId);

    /**
    * Admin confirms the money and access begins.
    *
    * The GitHub attempt happens here, and its outcome is recorded rather than
    * assumed. With the manual granter that always means "a human still has to
    * click Add people", which is not a failure - it is the queue this feature
    * runs on.
    */
    ProjectAccessVo approve(Long accessId, User admin);

    /** Admin confirms they have added the collaborator on GitHub. */
    ProjectAccessVo confirmCollaboratorAdded(Long accessId, User admin);

    ProjectAccessVo reject(Long accessId, String reason, User admin);

    /**
    * Take access away early.
    *
    * Also the action to use once access has expired: the row goes REVOKED with a
    * reason, and the granter is asked to remove the collaborator so somebody is
    * told to actually do it.
    */
    ProjectAccessVo revoke(Long accessId, String reason, User admin);

    long countAwaitingReview();

    long countActive();

    long countAwaitingInvite();

    String granterDescription();

    Path screenshotPath(Long accessId, User caller);

    String screenshotContentType(Long accessId);
}

package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.project.LiveProjectRequestDto;
import com.learn.interviewmentor.dto.project.ProjectAccessApplicationDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.project.LiveProjectVo;
import com.learn.interviewmentor.vo.project.ProjectAccessVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Live projects and contributor access, one method per use case.
 *
 * The composition this layer owns is worth pointing at: {@link #openProjects}
 * needs the catalogue *and* the caller's own access, because whether a project's
 * repository path is included depends on whether that person already holds it.
 * Neither service should have to know about the other to answer that.
 */
public interface ProjectFacade {

    // ---- browsing ----
    ApiResult<List<LiveProjectVo>> openProjects(User caller);

    ApiResult<LiveProjectVo> project(Long id, User caller);

    // ---- requesting access ----
    ApiResult<ProjectAccessVo> apply(Long projectId, ProjectAccessApplicationDto request, User student);

    ApiResult<List<ProjectAccessVo>> myAccess(User student);

    ApiResult<PaymentInstructionsVo> paymentInstructions(Long accessId, User caller);

    ApiResult<ProjectAccessVo> submitProof(Long accessId, String upiReference,
                                          MultipartFile screenshot, User student);

    ApiResult<ProjectAccessVo> cancel(Long accessId, User student);

    ApiResult<ProjectAccessVo> changeGithubUsername(Long accessId, String githubUsername, User student);

    /** Bytes, not an envelope - see PlanFacade#screenshotPath. */
    Path screenshotPath(Long accessId, User caller);

    String screenshotContentType(Long accessId);

    // ---- admin: the catalogue ----
    ApiResult<List<LiveProjectVo>> allProjects();

    ApiResult<LiveProjectVo> create(LiveProjectRequestDto request);

    ApiResult<LiveProjectVo> update(Long id, LiveProjectRequestDto request);

    ApiResult<LiveProjectVo> changePrice(Long id, PlanPriceRequestDto request);

    ApiResult<LiveProjectVo> setActive(Long id, boolean active);

    ApiResult<List<ProjectAccessVo>> contributorsOn(Long projectId);

    // ---- admin: access requests ----
    ApiResult<List<ProjectAccessVo>> pendingAccess();

    /** Paid and approved, but nobody has added them on GitHub yet. */
    ApiResult<List<ProjectAccessVo>> awaitingInvite();

    /** Still ACTIVE but past expiry - these people should be off the repo. */
    ApiResult<List<ProjectAccessVo>> pastExpiry();

    ApiResult<List<ProjectAccessVo>> allAccess();

    ApiResult<ProjectAccessVo> approve(Long accessId, User admin);

    ApiResult<ProjectAccessVo> confirmCollaboratorAdded(Long accessId, User admin);

    ApiResult<ProjectAccessVo> reject(Long accessId, String reason, User admin);

    ApiResult<ProjectAccessVo> revoke(Long accessId, String reason, User admin);
}

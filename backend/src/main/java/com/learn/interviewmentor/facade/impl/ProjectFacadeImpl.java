package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.project.LiveProjectRequestDto;
import com.learn.interviewmentor.dto.project.ProjectAccessApplicationDto;
import com.learn.interviewmentor.facade.ProjectFacade;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.LiveProjectService;
import com.learn.interviewmentor.service.ProjectAccessService;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.project.LiveProjectVo;
import com.learn.interviewmentor.vo.project.ProjectAccessVo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Component
public class ProjectFacadeImpl implements ProjectFacade {

    private final LiveProjectService projectService;
    private final ProjectAccessService accessService;

    public ProjectFacadeImpl(LiveProjectService projectService,
                             ProjectAccessService accessService) {
        this.projectService = projectService;
        this.accessService = accessService;
    }

    // ---- browsing ----

    /**
     * The catalogue, with each project's repository included only if this caller
     * already holds access to it.
     *
     * This is the composition that justifies the layer: it takes the catalogue
     * from one service and the caller's access from another, and neither needs to
     * know the other exists.
     */
    @Override
    public ApiResult<List<LiveProjectVo>> openProjects(User caller) {
        List<Long> mine = caller.getRole() == Role.STUDENT
                ? accessService.activeProjectIds(caller)
                : List.of();
        return ApiResult.ok(projectService.openProjects(mine));
    }

    @Override
    public ApiResult<LiveProjectVo> project(Long id, User caller) {
        boolean includeRepo = caller.getRole() == Role.ADMIN
                || accessService.activeProjectIds(caller).contains(id);
        return ApiResult.ok(projectService.one(id, includeRepo));
    }

    // ---- requesting access ----

    @Override
    public ApiResult<ProjectAccessVo> apply(Long projectId, ProjectAccessApplicationDto request,
                                           User student) {
        return ApiResult.created(accessService.apply(projectId, request, student));
    }

    @Override
    public ApiResult<List<ProjectAccessVo>> myAccess(User student) {
        return ApiResult.ok(accessService.mine(student));
    }

    @Override
    public ApiResult<PaymentInstructionsVo> paymentInstructions(Long accessId, User caller) {
        return ApiResult.ok(accessService.instructionsFor(accessId, caller));
    }

    @Override
    public ApiResult<ProjectAccessVo> submitProof(Long accessId, String upiReference,
                                                  MultipartFile screenshot, User student) {
        return ApiResult.ok(
                accessService.submitProof(accessId, upiReference, screenshot, student),
                "Thanks - we're checking your payment. You'll be added to the repository once "
                        + "an admin confirms it.");
    }

    @Override
    public ApiResult<ProjectAccessVo> cancel(Long accessId, User student) {
        return ApiResult.ok(accessService.cancel(accessId, student), "Request cancelled.");
    }

    @Override
    public ApiResult<ProjectAccessVo> changeGithubUsername(Long accessId, String githubUsername,
                                                           User student) {
        var saved = accessService.changeGithubUsername(accessId, githubUsername, student);
        return ApiResult.ok(saved, "GitHub username updated to @" + saved.githubUsername() + ".");
    }

    @Override
    public Path screenshotPath(Long accessId, User caller) {
        return accessService.screenshotPath(accessId, caller);
    }

    @Override
    public String screenshotContentType(Long accessId) {
        return accessService.screenshotContentType(accessId);
    }

    // ---- admin: the catalogue ----

    @Override
    public ApiResult<List<LiveProjectVo>> allProjects() {
        return ApiResult.ok(projectService.allProjects());
    }

    @Override
    public ApiResult<LiveProjectVo> create(LiveProjectRequestDto request) {
        return ApiResult.created(projectService.create(request));
    }

    @Override
    public ApiResult<LiveProjectVo> update(Long id, LiveProjectRequestDto request) {
        return ApiResult.ok(projectService.update(id, request), "Project saved.");
    }

    @Override
    public ApiResult<LiveProjectVo> changePrice(Long id, PlanPriceRequestDto request) {
        var project = projectService.changePrice(id, request);
        return ApiResult.ok(project, project.name() + " is now ₹" + project.price()
                + ". Anyone who already has access keeps the price they paid.");
    }

    @Override
    public ApiResult<LiveProjectVo> setActive(Long id, boolean active) {
        var project = projectService.setActive(id, active);
        return ApiResult.ok(project, active
                ? project.name() + " is open to new contributors."
                : project.name() + " is closed to new contributors. Existing access is unaffected.");
    }

    @Override
    public ApiResult<List<ProjectAccessVo>> contributorsOn(Long projectId) {
        return ApiResult.ok(accessService.contributorsOn(projectId));
    }

    // ---- admin: access requests ----

    @Override
    public ApiResult<List<ProjectAccessVo>> pendingAccess() {
        return ApiResult.ok(accessService.awaitingReview());
    }

    @Override
    public ApiResult<List<ProjectAccessVo>> awaitingInvite() {
        return ApiResult.ok(accessService.awaitingCollaboratorInvite());
    }

    @Override
    public ApiResult<List<ProjectAccessVo>> pastExpiry() {
        return ApiResult.ok(accessService.pastExpiry());
    }

    @Override
    public ApiResult<List<ProjectAccessVo>> allAccess() {
        return ApiResult.ok(accessService.all());
    }

    /**
     * The message here carries the GitHub next-step, because with the manual
     * granter approving is only half the job - and an admin who walks away
     * thinking it is done leaves a paying student locked out.
     */
    @Override
    public ApiResult<ProjectAccessVo> approve(Long accessId, User admin) {
        var access = accessService.approve(accessId, admin);
        String message = access.collaboratorGranted()
                ? access.studentName() + " now has access to " + access.repoFullName() + "."
                : "Payment confirmed. " + access.grantError();
        return ApiResult.ok(access, message);
    }

    @Override
    public ApiResult<ProjectAccessVo> confirmCollaboratorAdded(Long accessId, User admin) {
        var access = accessService.confirmCollaboratorAdded(accessId, admin);
        return ApiResult.ok(access,
                "@" + access.githubUsername() + " is now a contributor on " + access.projectName() + ".");
    }

    @Override
    public ApiResult<ProjectAccessVo> reject(Long accessId, String reason, User admin) {
        return ApiResult.ok(accessService.reject(accessId, reason, admin),
                "Rejected. The student can send new proof.");
    }

    @Override
    public ApiResult<ProjectAccessVo> revoke(Long accessId, String reason, User admin) {
        var access = accessService.revoke(accessId, reason, admin);
        return ApiResult.ok(access,
                "Access revoked. Remember to remove @" + access.githubUsername()
                        + " from the repository on GitHub.");
    }
}

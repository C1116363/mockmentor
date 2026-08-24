package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.dto.project.ProjectAccessApplicationDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.github.CollaboratorGranter;
import com.learn.interviewmentor.model.LiveProject;
import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.ProjectAccessRequest;
import com.learn.interviewmentor.model.ProjectAccessStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.payment.PurchaseSettlement;
import com.learn.interviewmentor.repository.ProjectAccessRequestRepository;
import com.learn.interviewmentor.service.LiveProjectService;
import com.learn.interviewmentor.service.ProjectAccessService;
import com.learn.interviewmentor.storage.ScreenshotStorage;
import com.learn.interviewmentor.vo.payment.PaymentInstructionsVo;
import com.learn.interviewmentor.vo.project.ProjectAccessVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * The same manual-UPI flow as plans, with one thing that makes it materially
 * different - approving this has an effect outside our database. A plan going
 * ACTIVE changes what our screens show. This going ACTIVE means a real GitHub
 * account gains push access to a real private repository.
 *
 * <h2>Why payment and the GitHub invite are recorded separately</h2>
 * {@code approve()} confirms the money and starts the clock. Whether the invite
 * actually landed is {@code collaboratorGranted}, set separately. Conflating them
 * would mean a failed invite reads as granted access - so a student is told they
 * can contribute, finds a 404 on the repo, and nothing in our system knows
 * anything is wrong. Keeping them apart turns that into a visible queue instead.
 *
 * <h2>Seats are checked twice</h2>
 * At request time and again at approval time. Days can pass between the two, and
 * the last seat can go in between. Checking only at the start oversells; checking
 * only at the end means taking money for a seat that was never there.
 */
@Service
@Transactional(readOnly = true)
public class ProjectAccessServiceImpl implements ProjectAccessService, PurchaseSettlement {

    private static final Logger log = LoggerFactory.getLogger(ProjectAccessServiceImpl.class);

    private final ProjectAccessRequestRepository accessRepository;
    private final LiveProjectService projectService;
    private final ScreenshotStorage storage;
    private final CollaboratorGranter granter;

    private final String upiId;
    private final String payeeName;

    public ProjectAccessServiceImpl(ProjectAccessRequestRepository accessRepository,
                                    LiveProjectService projectService,
                                    ScreenshotStorage storage,
                                    CollaboratorGranter granter,
                                    @Value("${app.payment.upi-id}") String upiId,
                                    @Value("${app.payment.payee-name}") String payeeName) {
        this.accessRepository = accessRepository;
        this.projectService = projectService;
        this.storage = storage;
        this.granter = granter;
        this.upiId = upiId;
        this.payeeName = payeeName;
    }

    // ---------------- student ----------------

    @Override
    @Transactional
    public ProjectAccessVo apply(Long projectId, ProjectAccessApplicationDto dto, User student) {
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can request project access");
        }

        LiveProject project = projectService.openEntity(projectId);

        var existing = accessRepository.findLatestLive(student.getId(), projectId);
        if (existing.isPresent()) {
            ProjectAccessRequest live = existing.get();
            if (live.isCurrentlyActive()) {
                throw new ConflictException(
                        "You already have access to " + project.getName() + ".");
            }
            // Mid-request already. Let them correct a mistyped handle rather than
            // stranding them with a request that can never be granted.
            live.setGithubUsername(dto.githubUsername().trim());
            return ProjectAccessVo.from(live);
        }

        projectService.assertSeatAvailable(project);

        ProjectAccessRequest saved = accessRepository.save(new ProjectAccessRequest(
                project, student, dto.githubUsername().trim(), trimOrNull(dto.motivation())));

        log.info("{} requested access to '{}' as GitHub user '{}' at {}",
                student.getEmail(), project.getName(), saved.getGithubUsername(),
                saved.getPricePaid());
        return ProjectAccessVo.from(saved);
    }

    @Override
    public List<ProjectAccessVo> mine(User student) {
        return accessRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(ProjectAccessVo::from).toList();
    }

    /** Project ids this student currently holds. Drives repo visibility in the catalogue. */
    @Override
    public List<Long> activeProjectIds(User student) {
        return accessRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(ProjectAccessRequest::isCurrentlyActive)
                .map(r -> r.getProject().getId())
                .toList();
    }

    @Override
    public PaymentInstructionsVo instructionsFor(Long accessId, User caller) {
        ProjectAccessRequest request = getOrThrow(accessId);
        assertCanSee(request, caller);

        BigDecimal amount = request.getPricePaid();
        String link = "upi://pay"
                + "?pa=" + enc(upiId)
                + "&pn=" + enc(payeeName)
                + "&am=" + amount.toPlainString()
                + "&cu=INR";
        return new PaymentInstructionsVo(upiId, payeeName, amount, "INR", link);
    }

    @Override
    @Transactional
    public ProjectAccessVo submitProof(Long accessId, String upiReference,
                                       MultipartFile screenshot, User student) {
        ProjectAccessRequest request = getOrThrow(accessId);

        if (!request.isOwnedBy(student)) {
            throw new ForbiddenException("That isn't your request");
        }
        if (upiReference == null || upiReference.isBlank()) {
            throw new BadRequestException("Enter the UPI transaction / UTR number");
        }

        switch (request.getStatus()) {
            case ACTIVE -> throw new ConflictException("You have already paid for this access");
            case SUBMITTED -> throw new ConflictException("We're already checking your last screenshot");
            case CANCELLED -> throw new ConflictException("This request was cancelled. Start again.");
            case REVOKED -> throw new ConflictException(
                    "This access was revoked. Contact an admin before requesting again.");
            default -> { /* AWAITING_PAYMENT, REJECTED and EXPIRED may all submit */ }
        }

        String filename = storage.store(screenshot);
        request.submitProof(upiReference.trim(), filename, storage.contentTypeOf(screenshot));
        log.info("{} submitted payment proof for project access '{}'",
                student.getEmail(), request.getProject().getName());
        return ProjectAccessVo.from(request);
    }

    @Override
    @Transactional
    public ProjectAccessVo cancel(Long accessId, User student) {
        ProjectAccessRequest request = getOrThrow(accessId);
        if (!request.isOwnedBy(student)) {
            throw new ForbiddenException("That isn't your request");
        }
        if (request.getStatus() == ProjectAccessStatus.ACTIVE) {
            throw new ConflictException(
                    "Your access is already live. Contact an admin if you need it stopped.");
        }
        request.cancel();
        return ProjectAccessVo.from(request);
    }

    /** Fix a mistyped handle before it has been granted. */
    @Override
    @Transactional
    public ProjectAccessVo changeGithubUsername(Long accessId, String githubUsername, User student) {
        ProjectAccessRequest request = getOrThrow(accessId);
        if (!request.isOwnedBy(student)) {
            throw new ForbiddenException("That isn't your request");
        }
        if (request.isCurrentlyActive() && request.isCollaboratorGranted()) {
            // The old handle is already a collaborator. Silently swapping the
            // column would leave that access in place with nothing pointing at it.
            throw new ConflictException(
                    "Access has already been granted to @" + request.getGithubUsername()
                            + ". Ask an admin to change it, so the old account can be removed too.");
        }
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new BadRequestException("Enter your GitHub username");
        }
        request.setGithubUsername(githubUsername.trim());
        return ProjectAccessVo.from(request);
    }

    // ---------------- admin ----------------

    @Override
    public List<ProjectAccessVo> awaitingReview() {
        return accessRepository.findByStatusOrderBySubmittedAtAsc(ProjectAccessStatus.SUBMITTED)
                .stream().map(ProjectAccessVo::from).toList();
    }

    /** Paid and approved, but nobody has added them on GitHub yet. */
    @Override
    public List<ProjectAccessVo> awaitingCollaboratorInvite() {
        return accessRepository.findAwaitingCollaboratorInvite(LocalDateTime.now())
                .stream().map(ProjectAccessVo::from).toList();
    }

    /** Still ACTIVE but past expiry - these people should be off the repo. */
    @Override
    public List<ProjectAccessVo> pastExpiry() {
        return accessRepository.findPastExpiry(LocalDateTime.now())
                .stream().map(ProjectAccessVo::from).toList();
    }

    @Override
    public List<ProjectAccessVo> all() {
        return accessRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ProjectAccessVo::from).toList();
    }

    @Override
    public List<ProjectAccessVo> contributorsOn(Long projectId) {
        return accessRepository.findActiveContributors(projectId, LocalDateTime.now())
                .stream().map(ProjectAccessVo::from).toList();
    }

    /**
     * Admin confirms the money and access begins.
     *
     * The GitHub attempt happens here, and its outcome is recorded rather than
     * assumed. With the manual granter that always means "a human still has to
     * click Add people", which is not a failure - it is the queue this feature
     * runs on.
     */
    @Override
    @Transactional
    public ProjectAccessVo approve(Long accessId, User admin) {
        ProjectAccessRequest request = getOrThrow(accessId);

        if (request.getStatus() != ProjectAccessStatus.SUBMITTED) {
            throw new ConflictException("Only a SUBMITTED request can be approved. This one is "
                    + request.getStatus() + ".");
        }

        // Re-checked here, not just at request time - days can pass and the last
        // seat can go in between. Excluding this request's own id matters: it is
        // SUBMITTED, so it is already inside the seat count, and without the
        // exclusion a full project would refuse to approve the request holding
        // one of its own seats.
        projectService.assertSeatAvailable(request.getProject(), request.getId());

        request.approve(admin);

        var result = granter.grant(
                request.getProject().getRepoFullName(),
                request.getGithubUsername(),
                CollaboratorGranter.PUSH);

        if (result.done()) {
            request.markCollaboratorGranted();
        } else {
            request.markGrantFailed(result.message());
        }

        log.info("Project access {} approved for {} on '{}' until {} - GitHub: {}",
                accessId, request.getStudent().getEmail(),
                request.getProject().getRepoFullName(), request.getExpiresAt(), result.message());

        return ProjectAccessVo.from(request);
    }

    /** Admin confirms they have added the collaborator on GitHub. */
    @Override
    @Transactional
    public ProjectAccessVo confirmCollaboratorAdded(Long accessId, User admin) {
        ProjectAccessRequest request = getOrThrow(accessId);
        if (!request.isCurrentlyActive()) {
            throw new ConflictException(
                    "This access is not active, so there is nothing to confirm.");
        }
        request.markCollaboratorGranted();
        log.info("{} confirmed '{}' was added to {}",
                admin.getEmail(), request.getGithubUsername(),
                request.getProject().getRepoFullName());
        return ProjectAccessVo.from(request);
    }

    @Override
    @Transactional
    public ProjectAccessVo reject(Long accessId, String reason, User admin) {
        ProjectAccessRequest request = getOrThrow(accessId);
        if (request.getStatus() != ProjectAccessStatus.SUBMITTED) {
            throw new ConflictException("Only a SUBMITTED request can be rejected. This one is "
                    + request.getStatus() + ".");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Tell the student why, so they can fix it");
        }
        request.reject(admin, reason.trim());
        return ProjectAccessVo.from(request);
    }

    /**
     * Take access away early.
     *
     * Also the action to use once access has expired: the row goes REVOKED with a
     * reason, and the granter is asked to remove the collaborator so somebody is
     * told to actually do it.
     */
    @Override
    @Transactional
    public ProjectAccessVo revoke(Long accessId, String reason, User admin) {
        ProjectAccessRequest request = getOrThrow(accessId);

        if (request.getStatus() != ProjectAccessStatus.ACTIVE) {
            throw new ConflictException("Only ACTIVE access can be revoked. This one is "
                    + request.getStatus() + ".");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Give a reason - the student sees it");
        }

        var result = granter.revoke(
                request.getProject().getRepoFullName(), request.getGithubUsername());

        request.revoke(reason.trim());

        log.info("{} revoked project access {} ({} on {}): {} - GitHub: {}",
                admin.getEmail(), accessId, request.getGithubUsername(),
                request.getProject().getRepoFullName(), reason, result.message());

        return ProjectAccessVo.from(request);
    }

    @Override
    public long countAwaitingReview() {
        return accessRepository.countByStatus(ProjectAccessStatus.SUBMITTED);
    }

    @Override
    public long countActive() {
        return accessRepository.countByStatus(ProjectAccessStatus.ACTIVE);
    }

    @Override
    public long countAwaitingInvite() {
        return accessRepository.findAwaitingCollaboratorInvite(LocalDateTime.now()).size();
    }

    @Override
    public String granterDescription() {
        return granter.describe();
    }

    // ---------------- screenshot ----------------

    @Override
    public Path screenshotPath(Long accessId, User caller) {
        ProjectAccessRequest request = getOrThrow(accessId);
        assertCanSee(request, caller);

        if (request.getScreenshotFile() == null) {
            throw new NotFoundException("No screenshot uploaded for this request");
        }

        Path path = storage.pathOf(request.getScreenshotFile());

        // Checked before the controller starts streaming: once the headers are
        // sent the status cannot change, and a missing file would give the client
        // a truncated body instead of a 404.
        if (!Files.isReadable(path)) {
            throw new NotFoundException("That screenshot is no longer available");
        }
        return path;
    }

    @Override
    public String screenshotContentType(Long accessId) {
        return getOrThrow(accessId).getScreenshotContentType();
    }

    // ---------------- helpers ----------------

    // ---------- gateway settlement ----------

    @Override
    public PaymentPurpose purpose() {
        return PaymentPurpose.PROJECT;
    }

    /**
     * The seat check runs here, before the money.
     *
     * A project has a fixed number of contributor seats and days can pass
     * between requesting one and paying for it. Checking at the checkout means
     * the student is told "this project filled up" instead of being charged and
     * refunded - and a refund on a card takes five working days to come back,
     * which is a genuinely bad experience over a race we can see coming.
     *
     * It cannot be the only check. Two students can both pass this and both pay
     * before either settles, so {@link #settle} looks again. What this buys is
     * that the overwhelmingly common case fails cheaply and early.
     */
    @Override
    public PurchaseSettlement.Payable prepare(Long accessId, User caller) {
        ProjectAccessRequest request = getOrThrow(accessId);

        if (!request.isOwnedBy(caller)) {
            throw new ForbiddenException("That isn't your request");
        }
        if (request.getStatus() == ProjectAccessStatus.ACTIVE) {
            throw new ConflictException("You already have access to this project.");
        }
        if (request.getStatus() == ProjectAccessStatus.SUBMITTED) {
            throw new ConflictException(
                    "We're already checking the screenshot you sent. Wait for that to be "
                            + "reviewed rather than paying twice.");
        }
        if (request.getStatus() == ProjectAccessStatus.CANCELLED) {
            throw new ConflictException("You cancelled this request. Apply again to restart it.");
        }

        projectService.assertSeatAvailable(request.getProject(), request.getId());

        return new PurchaseSettlement.Payable(
                request.getPricePaid(),
                "Contributor access - " + request.getProject().getName());
    }

    /**
     * Paid. Grant access, and do not throw if GitHub says no.
     *
     * <h2>The seat re-check does not roll anything back</h2>
     * If the last seat went while this student was at the checkout, their money
     * is already taken - refusing here would leave them charged with nothing to
     * show for it, and the gateway would redeliver the webhook forever trying to
     * tell us about a payment we keep rejecting. So the access is granted and
     * the overfill is logged loudly for an admin to sort out. One seat over is a
     * conversation; a silent unrefunded charge is not.
     *
     * <h2>Why a failed GitHub invite is not a failure here</h2>
     * Same reason. The student paid, so they get the row, the expiry window and
     * the entry in the admin's "needs inviting" queue - which is exactly where a
     * manual approval leaves it too. A throw would undo the settlement and put
     * the payment back in limbo over something a person fixes in ten seconds.
     */
    @Transactional
    @Override
    public void settle(Long accessId, String gatewayPaymentId) {
        ProjectAccessRequest request = getOrThrow(accessId);

        try {
            projectService.assertSeatAvailable(request.getProject(), request.getId());
        } catch (RuntimeException e) {
            log.warn("Project '{}' is over its seat limit: {} paid ({}) for access {} after "
                            + "the last seat went. Access granted anyway - review this.",
                    request.getProject().getRepoFullName(), request.getStudent().getEmail(),
                    gatewayPaymentId, accessId);
        }

        request.settleByGateway(gatewayPaymentId);

        var result = granter.grant(
                request.getProject().getRepoFullName(),
                request.getGithubUsername(),
                CollaboratorGranter.PUSH);

        if (result.done()) {
            request.markCollaboratorGranted();
        } else {
            request.markGrantFailed(result.message());
        }

        log.info("Project access {} settled by gateway payment {} for {} on '{}' until {} - GitHub: {}",
                accessId, gatewayPaymentId, request.getStudent().getEmail(),
                request.getProject().getRepoFullName(), request.getExpiresAt(), result.message());
    }

    private void assertCanSee(ProjectAccessRequest request, User caller) {
        if (caller.getRole() != Role.ADMIN && !request.isOwnedBy(caller)) {
            throw new ForbiddenException("That isn't your request");
        }
    }

    private ProjectAccessRequest getOrThrow(Long id) {
        return accessRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No access request with id " + id));
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

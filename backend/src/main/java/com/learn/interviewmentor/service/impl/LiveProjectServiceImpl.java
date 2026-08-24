package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.project.LiveProjectRequestDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.LiveProject;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.LiveProjectRepository;
import com.learn.interviewmentor.repository.ProjectAccessRequestRepository;
import com.learn.interviewmentor.repository.UserRepository;
import com.learn.interviewmentor.service.LiveProjectService;
import com.learn.interviewmentor.vo.project.LiveProjectVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The catalogue of private codebases students can pay to contribute to.
 *
 * Two things this class is careful about:
 *
 * <ol>
 *   <li><b>Seat counts come from the access table, never from a column here.</b>
 *       A cached counter on the project row is one missed decrement away from
 *       selling a seat that does not exist, and the failure only shows up when a
 *       contributor is told the project is full after paying.</li>
 *   <li><b>The repository path is not handed to browsers.</b> These repos are
 *       private; the VO has two builders and this class picks the withholding one
 *       for anybody without live access.</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class LiveProjectServiceImpl implements LiveProjectService {

    private static final Logger log = LoggerFactory.getLogger(LiveProjectServiceImpl.class);

    private final LiveProjectRepository projectRepository;
    private final ProjectAccessRequestRepository accessRepository;
    private final UserRepository userRepository;

    public LiveProjectServiceImpl(LiveProjectRepository projectRepository,
                                  ProjectAccessRequestRepository accessRepository,
                                  UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.accessRepository = accessRepository;
        this.userRepository = userRepository;
    }

    // ---------- reading ----------

    /**
     * The browsing catalogue: open projects, repository withheld.
     *
     * @param withAccessTo project ids this caller currently holds - those get the
     *                     repo included, since they are already a collaborator.
     */
    @Override
    public List<LiveProjectVo> openProjects(List<Long> withAccessTo) {
        LocalDateTime now = LocalDateTime.now();
        return projectRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(p -> withAccessTo.contains(p.getId())
                        ? LiveProjectVo.forContributor(p, seatsTaken(p, now))
                        : LiveProjectVo.forBrowsing(p, seatsTaken(p, now)))
                .toList();
    }

    /** The admin list: retired projects included, repository always shown. */
    @Override
    public List<LiveProjectVo> allProjects() {
        LocalDateTime now = LocalDateTime.now();
        return projectRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(p -> LiveProjectVo.forContributor(p, seatsTaken(p, now)))
                .toList();
    }

    @Override
    public LiveProjectVo one(Long id, boolean includeRepo) {
        LiveProject project = getOrThrow(id);
        long taken = seatsTaken(project, LocalDateTime.now());
        return includeRepo
                ? LiveProjectVo.forContributor(project, taken)
                : LiveProjectVo.forBrowsing(project, taken);
    }

    /** The entity, for the access service. Students can only apply to a live one. */
    @Override
    public LiveProject openEntity(Long id) {
        LiveProject project = getOrThrow(id);
        if (!project.isActive()) {
            throw new ConflictException("That project is not accepting contributors right now");
        }
        return project;
    }

    /**
     * Whether one more contributor can be taken on.
     *
     * Checked at request time and again at approval time - the gap between a
     * student clicking and an admin verifying can be days, and the last seat can
     * go in between.
     */
    @Override
    public void assertSeatAvailable(LiveProject project) {
        assertSeatAvailable(project, null);
    }

    /**
     * @param excludingRequestId the request being approved, which already holds a
     *                           seat in the count and must not compete with
     *                           itself. Null when checking a brand-new request.
     */
    @Override
    public void assertSeatAvailable(LiveProject project, Long excludingRequestId) {
        Integer max = project.getMaxContributors();
        if (max == null) {
            return;
        }
        long taken = accessRepository.countTakenSeats(
                project.getId(), LocalDateTime.now(),
                excludingRequestId == null ? -1L : excludingRequestId);

        if (taken >= max) {
            throw new ConflictException(project.getName() + " is full ("
                    + taken + " of " + max + " contributor seats taken). "
                    + "Seats free up as access expires - check back.");
        }
    }

    // ---------- admin writes ----------

    @Override
    @Transactional
    public LiveProjectVo create(LiveProjectRequestDto dto) {
        String name = dto.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A project called \"" + name + "\" already exists");
        }
        if (projectRepository.existsByRepoOwnerIgnoreCaseAndRepoNameIgnoreCase(
                dto.repoOwner().trim(), dto.repoName().trim())) {
            throw new ConflictException("Another project already points at "
                    + dto.repoOwner().trim() + "/" + dto.repoName().trim());
        }

        LiveProject project = new LiveProject(
                name,
                trimOrNull(dto.summary()),
                trimOrNull(dto.description()),
                trimOrNull(dto.techStack()),
                trimOrNull(dto.sampleTasks()),
                dto.repoOwner().trim(),
                dto.repoName().trim(),
                dto.price(),
                dto.accessDurationDays() == 0 ? 90 : dto.accessDurationDays(),
                dto.maxContributors(),
                dto.difficulty(),
                dto.displayOrder());
        project.setOnboardingUrl(trimOrNull(dto.onboardingUrl()));
        project.setActive(dto.active());
        applyReviewer(project, dto.leadReviewerId());

        LiveProject saved = projectRepository.save(project);
        log.info("Live project created: '{}' -> {} at {}",
                saved.getName(), saved.getRepoFullName(), saved.getPrice());
        return LiveProjectVo.forContributor(saved, 0);
    }

    @Override
    @Transactional
    public LiveProjectVo update(Long id, LiveProjectRequestDto dto) {
        LiveProject project = getOrThrow(id);
        String name = dto.name().trim();

        projectRepository.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("A project called \"" + name + "\" already exists");
            }
        });

        // Changing the repo of a project people already contribute to would
        // silently point their access at a different codebase - the row would say
        // they have access to the new repo, which nobody ever granted.
        boolean repoChanged = !project.getRepoOwner().equalsIgnoreCase(dto.repoOwner().trim())
                || !project.getRepoName().equalsIgnoreCase(dto.repoName().trim());
        if (repoChanged) {
            long active = accessRepository.findActiveContributors(id, LocalDateTime.now()).size();
            if (active > 0) {
                throw new ConflictException(
                        "You cannot change the repository while " + active + " contributor(s) "
                                + "still have access. Revoke their access first, or make a new project.");
            }
        }

        project.setName(name);
        project.setSummary(trimOrNull(dto.summary()));
        project.setDescription(trimOrNull(dto.description()));
        project.setTechStack(trimOrNull(dto.techStack()));
        project.setSampleTasks(trimOrNull(dto.sampleTasks()));
        project.setRepoOwner(dto.repoOwner().trim());
        project.setRepoName(dto.repoName().trim());
        project.setOnboardingUrl(trimOrNull(dto.onboardingUrl()));
        project.setPrice(dto.price());
        if (dto.accessDurationDays() > 0) {
            project.setAccessDurationDays(dto.accessDurationDays());
        }
        project.setMaxContributors(dto.maxContributors());
        project.setDifficulty(dto.difficulty());
        project.setDisplayOrder(dto.displayOrder());
        project.setActive(dto.active());
        applyReviewer(project, dto.leadReviewerId());

        log.info("Live project {} updated: '{}' -> {}", id, project.getName(), project.getRepoFullName());
        return LiveProjectVo.forContributor(project, seatsTaken(project, LocalDateTime.now()));
    }

    /** Just the price - the change an admin makes most often. */
    @Override
    @Transactional
    public LiveProjectVo changePrice(Long id, PlanPriceRequestDto dto) {
        LiveProject project = getOrThrow(id);
        var previous = project.getPrice();
        project.setPrice(dto.price());
        log.info("Live project {} ('{}') price changed {} -> {}",
                id, project.getName(), previous, dto.price());
        return LiveProjectVo.forContributor(project, seatsTaken(project, LocalDateTime.now()));
    }

    /**
     * Open or close a project to new contributors.
     *
     * Closing does **not** revoke anybody - people mid-contribution keep their
     * access until it expires. Taking code access away because a project stopped
     * selling would be a surprise nobody agreed to.
     */
    @Override
    @Transactional
    public LiveProjectVo setActive(Long id, boolean active) {
        LiveProject project = getOrThrow(id);
        project.setActive(active);
        log.info("Live project {} ('{}') is now {}",
                id, project.getName(), active ? "open" : "closed to new contributors");
        return LiveProjectVo.forContributor(project, seatsTaken(project, LocalDateTime.now()));
    }

    @Override
    public long openCount() {
        return projectRepository.countByActiveTrue();
    }

    // ---------- helpers ----------

    private long seatsTaken(LiveProject project, LocalDateTime now) {
        return accessRepository.countTakenSeats(project.getId(), now);
    }

    private void applyReviewer(LiveProject project, Long reviewerId) {
        if (reviewerId == null) {
            project.setLeadReviewer(null);
            return;
        }
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("No user with id " + reviewerId));
        // A student cannot be the person reviewing production pull requests.
        if (reviewer.getRole() == Role.STUDENT) {
            throw new BadRequestException(
                    reviewer.getFullName() + " is a student, so cannot be the lead reviewer");
        }
        project.setLeadReviewer(reviewer);
    }

    private LiveProject getOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No live project with id " + id));
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

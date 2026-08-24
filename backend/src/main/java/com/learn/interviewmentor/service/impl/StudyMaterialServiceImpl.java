package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.PlanEnrollmentService;
import com.learn.interviewmentor.service.PlanService;
import com.learn.interviewmentor.service.StudyMaterialService;

import com.learn.interviewmentor.dto.material.MaterialLinkRequestDto;
import com.learn.interviewmentor.vo.material.StudyMaterialVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.MaterialAudience;
import com.learn.interviewmentor.model.MaterialKind;
import com.learn.interviewmentor.model.Plan;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.StudyMaterial;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.StudyMaterialRepository;
import com.learn.interviewmentor.repository.UserRepository;
import com.learn.interviewmentor.storage.MaterialStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Study material: an admin sends notes, a PDF or a link to students.
 *
 * Three audiences, and the choice between them is the whole feature:
 *
 *  - ALL_STUDENTS     - everyone
 *  - SPECIFIC_STUDENT - one named student, and nobody else
 *  - PLAN_MEMBERS     - whoever currently holds a given plan
 *
 * <b>Every one of those is enforced in the query</b>, in
 * {@code StudyMaterialRepository.findVisibleTo}. Filtering on the client would
 * leave the rows sitting in a JSON response any student could read out of their
 * own network tab - and for SPECIFIC_STUDENT that is somebody else's private
 * material, which is a real leak whether or not a screen ever renders it.
 *
 * The same rule applies to the download: {@link #fileFor} re-checks visibility
 * rather than trusting that the caller only knows about ids they were shown.
 */
@Service
@Transactional(readOnly = true)
public class StudyMaterialServiceImpl implements StudyMaterialService {

    private static final Logger log = LoggerFactory.getLogger(StudyMaterialServiceImpl.class);

    /**
     * Stands in for "no plans" in the {@code in :planIds} clause.
     *
     * An empty collection makes Hibernate emit {@code in ()}, which MySQL
     * rejects as a syntax error. A single id that cannot exist keeps the SQL
     * valid and matches nothing, which is exactly what a student with no plans
     * should see.
     */
    private static final List<Long> NO_PLANS = List.of(-1L);

    private final StudyMaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final PlanService planService;
    private final PlanEnrollmentService enrollmentService;
    private final MaterialStorage storage;

    public StudyMaterialServiceImpl(StudyMaterialRepository materialRepository,
                                UserRepository userRepository,
                                PlanService planService,
                                PlanEnrollmentService enrollmentService,
                                MaterialStorage storage) {
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
        this.planService = planService;
        this.enrollmentService = enrollmentService;
        this.storage = storage;
    }

    // ---------- student ----------

    /** Everything this student may see, newest first. */
    @Override
    public List<StudyMaterialVo> visibleTo(User student) {
        List<Long> planIds = enrollmentService.activePlanIds(student);
        return materialRepository
                .findVisibleTo(student.getId(), planIds.isEmpty() ? NO_PLANS : planIds)
                .stream().map(StudyMaterialVo::from).toList();
    }

    // ---------- admin ----------

    @Override
    public List<StudyMaterialVo> all() {
        return materialRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(StudyMaterialVo::from).toList();
    }

    /**
     * Upload a file and send it.
     *
     * @param targetStudentId set to send it to one student only
     * @param targetPlanId    set to send it to holders of one plan only
     */
    @Transactional
    @Override
    public StudyMaterialVo upload(String title, String description, MultipartFile file,
                                   Long targetStudentId, Long targetPlanId, User admin) {
        requireTitle(title);
        assertOneAudience(targetStudentId, targetPlanId);

        // Resolve the audience BEFORE writing the file. Otherwise a bad student
        // id leaves an orphaned upload on disk that nothing will ever reference
        // or clean up.
        User targetStudent = targetStudentId == null ? null : resolveStudent(targetStudentId);
        Plan targetPlan = targetPlanId == null ? null : planService.activeEntity(targetPlanId);

        String stored = storage.store(file);
        StudyMaterial material = StudyMaterial.file(
                title.trim(),
                trimOrNull(description),
                stored,
                storage.safeDisplayName(file),
                storage.contentTypeOf(file),
                file.getSize(),
                admin);

        applyAudience(material, targetStudent, targetPlan);

        StudyMaterial saved = materialRepository.save(material);
        log.info("Study material '{}' uploaded by {} for {}",
                saved.getTitle(), admin.getEmail(), saved.getAudience());
        return StudyMaterialVo.from(saved);
    }

    /** Share a link instead of a file. Nothing is stored on disk. */
    @Transactional
    @Override
    public StudyMaterialVo shareLink(MaterialLinkRequestDto dto,
                                      Long targetStudentId, Long targetPlanId, User admin) {
        assertOneAudience(targetStudentId, targetPlanId);

        User targetStudent = targetStudentId == null ? null : resolveStudent(targetStudentId);
        Plan targetPlan = targetPlanId == null ? null : planService.activeEntity(targetPlanId);

        StudyMaterial material = StudyMaterial.link(
                dto.title().trim(), trimOrNull(dto.description()), dto.linkUrl().trim(), admin);

        applyAudience(material, targetStudent, targetPlan);

        StudyMaterial saved = materialRepository.save(material);
        log.info("Study link '{}' shared by {} for {}",
                saved.getTitle(), admin.getEmail(), saved.getAudience());
        return StudyMaterialVo.from(saved);
    }

    /**
     * Publish or unpublish.
     *
     * There is no delete: an unpublished row keeps the file and the record of who
     * sent what to whom, and can be put back with one click if it was pulled by
     * mistake.
     */
    @Transactional
    @Override
    public StudyMaterialVo setActive(Long id, boolean active) {
        StudyMaterial material = getOrThrow(id);
        material.setActive(active);
        log.info("Study material {} ('{}') is now {}",
                id, material.getTitle(), active ? "published" : "hidden");
        return StudyMaterialVo.from(material);
    }

    @Override
    public long activeCount() {
        return materialRepository.countByActiveTrue();
    }

    // ---------- download ----------

    /**
     * The file, if this caller is allowed it.
     *
     * Visibility is re-checked here on purpose. The list endpoint decides what a
     * student is *shown*; it cannot decide what they *ask for*, and ids are
     * sequential integers. An admin bypasses the check because the admin screen
     * lists everything by design.
     */
    @Override
    public StudyMaterial fileFor(Long id, User caller) {
        StudyMaterial material = getOrThrow(id);

        if (material.getKind() != MaterialKind.FILE) {
            throw new BadRequestException("That material is a link, not a file");
        }
        if (caller.getRole() != Role.ADMIN && !canSee(material, caller)) {
            throw new ForbiddenException("That material was not shared with you");
        }
        return material;
    }

    /** Resolved and checked, so the controller can stream it straight out. */
    @Override
    public Path pathFor(StudyMaterial material) {
        Path path = storage.pathOf(material.getStoredFile());

        // Before streaming, not during: once the headers are sent the status
        // cannot change, and the client would get a truncated body rather than
        // a 404 it can act on.
        if (!Files.isReadable(path)) {
            throw new NotFoundException("That file is no longer available");
        }
        return path;
    }

    private boolean canSee(StudyMaterial material, User student) {
        if (!material.isActive()) {
            return false;
        }
        return switch (material.getAudience()) {
            case ALL_STUDENTS -> true;
            case SPECIFIC_STUDENT -> material.getTargetStudent() != null
                    && material.getTargetStudent().getId().equals(student.getId());
            case PLAN_MEMBERS -> material.getTargetPlan() != null
                    && enrollmentService.activePlanIds(student)
                    .contains(material.getTargetPlan().getId());
        };
    }

    private void applyAudience(StudyMaterial material, User targetStudent, Plan targetPlan) {
        if (targetStudent != null) {
            material.sendTo(targetStudent);
        } else if (targetPlan != null) {
            material.sendToPlan(targetPlan);
        } else {
            material.sendToEveryone();
        }
    }

    /**
     * A row has exactly one audience. Accepting both would leave the entity
     * describing something the {@link MaterialAudience} enum cannot represent,
     * and whichever one the code happened to check first would silently win.
     */
    private void assertOneAudience(Long targetStudentId, Long targetPlanId) {
        if (targetStudentId != null && targetPlanId != null) {
            throw new BadRequestException(
                    "Send it to one student or to a plan's members, not both");
        }
    }

    private User resolveStudent(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No user with id " + id));
        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException(user.getFullName() + " is not a student");
        }
        return user;
    }

    private void requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Give it a title");
        }
        if (title.trim().length() > 200) {
            throw new BadRequestException("Title must be 200 characters or fewer");
        }
    }

    private StudyMaterial getOrThrow(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No study material with id " + id));
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

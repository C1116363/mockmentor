package com.learn.interviewmentor.service;

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
public interface StudyMaterialService {

    /** Everything this student may see, newest first. */
    List<StudyMaterialVo> visibleTo(User student);

    List<StudyMaterialVo> all();

    /**
    * Upload a file and send it.
    *
    * @param targetStudentId set to send it to one student only
    * @param targetPlanId    set to send it to holders of one plan only
    */
    StudyMaterialVo upload(String title, String description, MultipartFile file, Long targetStudentId, Long targetPlanId, User admin);

    /** Share a link instead of a file. Nothing is stored on disk. */
    StudyMaterialVo shareLink(MaterialLinkRequestDto dto, Long targetStudentId, Long targetPlanId, User admin);

    /**
    * Publish or unpublish.
    *
    * There is no delete: an unpublished row keeps the file and the record of who
    * sent what to whom, and can be put back with one click if it was pulled by
    * mistake.
    */
    StudyMaterialVo setActive(Long id, boolean active);

    long activeCount();

    /**
    * The file, if this caller is allowed it.
    *
    * Visibility is re-checked here on purpose. The list endpoint decides what a
    * student is *shown*; it cannot decide what they *ask for*, and ids are
    * sequential integers. An admin bypasses the check because the admin screen
    * lists everything by design.
    */
    StudyMaterial fileFor(Long id, User caller);

    /** Resolved and checked, so the controller can stream it straight out. */
    Path pathFor(StudyMaterial material);
}

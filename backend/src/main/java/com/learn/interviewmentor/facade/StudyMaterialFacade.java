package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.material.MaterialLinkRequestDto;
import com.learn.interviewmentor.model.StudyMaterial;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.material.StudyMaterialVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/** Notes, files and links an admin sends to students. */
public interface StudyMaterialFacade {

    // ---- student ----
    ApiResult<List<StudyMaterialVo>> visibleTo(User student);

    /**
     * Returns the entity, not a VO.
     *
     * The controller needs the content type and the original filename to build
     * the download headers, and those are properties of the stored file rather
     * than of the response body. A VO here would exist only to carry three
     * fields into a header.
     */
    StudyMaterial fileFor(Long id, User caller);

    Path pathFor(StudyMaterial material);

    // ---- admin ----
    ApiResult<List<StudyMaterialVo>> all();

    ApiResult<StudyMaterialVo> upload(String title, String description, MultipartFile file,
                                      Long targetStudentId, Long targetPlanId, User admin);

    ApiResult<StudyMaterialVo> shareLink(MaterialLinkRequestDto request,
                                        Long targetStudentId, Long targetPlanId, User admin);

    ApiResult<StudyMaterialVo> setActive(Long id, boolean active);
}

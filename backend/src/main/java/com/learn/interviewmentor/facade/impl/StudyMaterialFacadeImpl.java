package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.material.MaterialLinkRequestDto;
import com.learn.interviewmentor.facade.StudyMaterialFacade;
import com.learn.interviewmentor.model.StudyMaterial;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.StudyMaterialService;
import com.learn.interviewmentor.vo.material.StudyMaterialVo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Component
public class StudyMaterialFacadeImpl implements StudyMaterialFacade {

    private final StudyMaterialService materialService;

    public StudyMaterialFacadeImpl(StudyMaterialService materialService) {
        this.materialService = materialService;
    }

    @Override
    public ApiResult<List<StudyMaterialVo>> visibleTo(User student) {
        return ApiResult.ok(materialService.visibleTo(student));
    }

    @Override
    public StudyMaterial fileFor(Long id, User caller) {
        return materialService.fileFor(id, caller);
    }

    @Override
    public Path pathFor(StudyMaterial material) {
        return materialService.pathFor(material);
    }

    @Override
    public ApiResult<List<StudyMaterialVo>> all() {
        return ApiResult.ok(materialService.all());
    }

    @Override
    public ApiResult<StudyMaterialVo> upload(String title, String description, MultipartFile file,
                                             Long targetStudentId, Long targetPlanId, User admin) {
        StudyMaterialVo saved =
                materialService.upload(title, description, file, targetStudentId, targetPlanId, admin);
        return ApiResult.created(saved);
    }

    @Override
    public ApiResult<StudyMaterialVo> shareLink(MaterialLinkRequestDto request,
                                                Long targetStudentId, Long targetPlanId, User admin) {
        StudyMaterialVo saved =
                materialService.shareLink(request, targetStudentId, targetPlanId, admin);
        return ApiResult.created(saved);
    }

    @Override
    public ApiResult<StudyMaterialVo> setActive(Long id, boolean active) {
        StudyMaterialVo saved = materialService.setActive(id, active);
        return ApiResult.ok(saved, active
                ? "Published - students can see it now."
                : "Hidden. The file is kept, and you can publish it again.");
    }
}

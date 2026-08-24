package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequestDto;
import com.learn.interviewmentor.facade.MentorFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.MentorProfileService;
import com.learn.interviewmentor.service.MentorService;
import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MentorFacadeImpl implements MentorFacade {

    private final MentorService mentorService;
    private final MentorProfileService profileService;

    public MentorFacadeImpl(MentorService mentorService, MentorProfileService profileService) {
        this.mentorService = mentorService;
        this.profileService = profileService;
    }

    @Override
    public ApiResult<List<MentorVo>> approvedMentors() {
        return ApiResult.ok(mentorService.findAll());
    }

    @Override
    public ApiResult<MentorVo> mentor(Long userId) {
        return ApiResult.ok(mentorService.findByUserId(userId));
    }

    @Override
    public ApiResult<MentorProfileVo> myProfile(User mentor) {
        return ApiResult.ok(profileService.myProfile(mentor));
    }

    @Override
    public ApiResult<MentorProfileVo> submitProfile(MentorProfileRequestDto request, User mentor) {
        return ApiResult.ok(profileService.submit(request, mentor),
                "Profile submitted. An admin will verify it before you appear to students.");
    }
}

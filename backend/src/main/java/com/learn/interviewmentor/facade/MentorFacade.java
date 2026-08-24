package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequestDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;

import java.util.List;

/** The mentor directory, and a mentor's own onboarding profile. */
public interface MentorFacade {

    ApiResult<List<MentorVo>> approvedMentors();

    ApiResult<MentorVo> mentor(Long userId);

    ApiResult<MentorProfileVo> myProfile(User mentor);

    ApiResult<MentorProfileVo> submitProfile(MentorProfileRequestDto request, User mentor);
}

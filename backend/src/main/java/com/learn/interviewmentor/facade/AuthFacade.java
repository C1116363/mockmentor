package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.auth.LoginRequestDto;
import com.learn.interviewmentor.dto.auth.SignupRequestDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.auth.AuthVo;
import com.learn.interviewmentor.vo.auth.UserVo;

/** Signing up, logging in, and "who am I". */
public interface AuthFacade {

    ApiResult<AuthVo> login(LoginRequestDto request);

    ApiResult<AuthVo> signupStudent(SignupRequestDto request);

    ApiResult<AuthVo> signupMentor(SignupRequestDto request);

    /** Reads the user off the token - no id parameter, so you can only ever see yourself. */
    ApiResult<UserVo> me(User caller);
}

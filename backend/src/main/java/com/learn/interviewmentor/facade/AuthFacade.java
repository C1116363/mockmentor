package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.auth.ForgotPasswordDto;
import com.learn.interviewmentor.dto.auth.LoginRequestDto;
import com.learn.interviewmentor.dto.auth.ResetPasswordDto;
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

    /**
     * "Send me a reset link."
     *
     * ApiResult<Void>, and always a success. There is no failure the caller is
     * allowed to see: a different answer for an unknown address would turn this
     * into a way of finding out who has an account here.
     */
    ApiResult<Void> forgotPassword(ForgotPasswordDto request);

    /** "Here's my link and my new password." */
    ApiResult<Void> resetPassword(ResetPasswordDto request);
}

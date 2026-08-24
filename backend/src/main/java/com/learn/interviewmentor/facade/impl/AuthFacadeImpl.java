package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.auth.LoginRequestDto;
import com.learn.interviewmentor.dto.auth.SignupRequestDto;
import com.learn.interviewmentor.facade.AuthFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.AuthService;
import com.learn.interviewmentor.vo.auth.AuthVo;
import com.learn.interviewmentor.vo.auth.UserVo;
import org.springframework.stereotype.Component;

@Component
public class AuthFacadeImpl implements AuthFacade {

    private final AuthService authService;

    public AuthFacadeImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ApiResult<AuthVo> login(LoginRequestDto request) {
        return ApiResult.ok(authService.login(request));
    }

    @Override
    public ApiResult<AuthVo> signupStudent(SignupRequestDto request) {
        return ApiResult.created(authService.signupStudent(request));
    }

    @Override
    public ApiResult<AuthVo> signupMentor(SignupRequestDto request) {
        return ApiResult.created(authService.signupMentor(request),
                "Account created. Fill in your profile next - an admin verifies it before you "
                        + "can take interviews.");
    }

    @Override
    public ApiResult<UserVo> me(User caller) {
        return ApiResult.ok(UserVo.from(caller));
    }
}

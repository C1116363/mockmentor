package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.auth.ForgotPasswordDto;
import com.learn.interviewmentor.dto.auth.LoginRequestDto;
import com.learn.interviewmentor.dto.auth.ResetPasswordDto;
import com.learn.interviewmentor.dto.auth.SignupRequestDto;
import com.learn.interviewmentor.facade.AuthFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.AuthService;
import com.learn.interviewmentor.service.PasswordResetService;
import com.learn.interviewmentor.vo.auth.AuthVo;
import com.learn.interviewmentor.vo.auth.UserVo;
import org.springframework.stereotype.Component;

@Component
public class AuthFacadeImpl implements AuthFacade {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthFacadeImpl(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
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

    /**
     * The message is written in the conditional on purpose.
     *
     * "If that address has an account" is doing real work: it is honest, it
     * tells the user what to do next, and it is the same sentence whether or
     * not an email went out - which is what stops this endpoint being used to
     * discover who is registered here.
     */
    @Override
    public ApiResult<Void> forgotPassword(ForgotPasswordDto request) {
        passwordResetService.requestReset(request);
        return ApiResult.ok(null,
                "If that address has an account, a reset link is on its way. "
                        + "It expires in 30 minutes - check your spam folder too.");
    }

    @Override
    public ApiResult<Void> resetPassword(ResetPasswordDto request) {
        passwordResetService.resetPassword(request);
        return ApiResult.ok(null,
                "Password changed. Log in with your new password - "
                        + "any other devices you were signed in on have been signed out.");
    }
}

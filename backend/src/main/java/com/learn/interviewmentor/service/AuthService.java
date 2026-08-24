package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.auth.AuthVo;
import com.learn.interviewmentor.dto.auth.LoginRequestDto;
import com.learn.interviewmentor.dto.auth.SignupRequestDto;
import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import com.learn.interviewmentor.repository.UserRepository;
import com.learn.interviewmentor.security.AppUserDetails;
import com.learn.interviewmentor.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Signup and login.
 *
 * There is deliberately no "signup as admin" endpoint. Letting anyone claim the
 * admin role over a public API would be a serious hole - the first admin is
 * created by the seeder, and after that an existing admin promotes people.
 */
public interface AuthService {

    AuthVo signupStudent(SignupRequestDto dto);

    /**
    * Creating the user and the profile in one @Transactional method means that
    * if the profile insert fails, the user insert rolls back too. You never end
    * up with a mentor account that has no profile.
    */
    AuthVo signupMentor(SignupRequestDto dto);

    AuthVo login(LoginRequestDto dto);
}

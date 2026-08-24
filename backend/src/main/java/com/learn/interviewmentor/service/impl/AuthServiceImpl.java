package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.AuthService;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signup and login.
 *
 * There is deliberately no "signup as admin" endpoint. Letting anyone claim the
 * admin role over a public API would be a serious hole - the first admin is
 * created by the seeder, and after that an existing admin promotes people.
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                       MentorProfileRepository mentorProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    @Override
    public AuthVo signupStudent(SignupRequestDto dto) {
        User user = createUser(dto.fullName(), dto.email(), dto.password(), Role.STUDENT);
        return buildResponse(user);
    }

    /**
     * Creating the user and the profile in one @Transactional method means that
     * if the profile insert fails, the user insert rolls back too. You never end
     * up with a mentor account that has no profile.
     */
    @Transactional
    @Override
    public AuthVo signupMentor(SignupRequestDto dto) {
        User user = createUser(dto.fullName(), dto.email(), dto.password(), Role.MENTOR);

        // A blank profile with status INCOMPLETE. The mentor fills it in after
        // logging in, and an admin verifies it before they can take interviews.
        mentorProfileRepository.save(new MentorProfile(user));

        return buildResponse(user);
    }

    @Override
    public AuthVo login(LoginRequestDto dto) {
        try {
            // This is what actually checks the BCrypt hash. It throws if the
            // email is unknown, the password is wrong, or the account is off.
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

            AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
            return buildResponse(principal.getUser());

        } catch (DisabledException ex) {
            throw new BadRequestException("This account has been deactivated. Contact an admin.");
        } catch (AuthenticationException ex) {
            // Deliberately vague: saying "no such email" would let an attacker
            // discover which emails are registered.
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    private User createUser(String fullName, String email, String rawPassword, Role role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with " + email + " already exists");
        }

        // The check above is a read followed by a write, so two signups for the
        // same address submitted at the same moment can both pass it. The unique
        // index on the column is what actually stops the second one, and it
        // surfaces as DataIntegrityViolationException. Catching it here means
        // the loser of that race gets the same 409 and the same sentence as
        // everyone else, instead of a 500 for a case the user can understand.
        try {
            // encode() is where the plain password stops existing.
            return userRepository.saveAndFlush(
                    new User(fullName, email, passwordEncoder.encode(rawPassword), role));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An account with " + email + " already exists");
        }
    }

    private AuthVo buildResponse(User user) {
        String token = jwtService.generateToken(new AppUserDetails(user));
        return new AuthVo(token, jwtService.getExpirationMillis(), UserVo.from(user));
    }
}

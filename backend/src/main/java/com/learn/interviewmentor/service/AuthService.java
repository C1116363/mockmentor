package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.auth.AuthResponse;
import com.learn.interviewmentor.dto.auth.LoginRequest;
import com.learn.interviewmentor.dto.auth.MentorSignupRequest;
import com.learn.interviewmentor.dto.auth.StudentSignupRequest;
import com.learn.interviewmentor.dto.auth.UserDto;
import com.learn.interviewmentor.exception.BadRequestException;
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
public class AuthService {

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
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
    public AuthResponse signupStudent(StudentSignupRequest dto) {
        User user = createUser(dto.fullName(), dto.email(), dto.password(), Role.STUDENT);
        return buildResponse(user);
    }

    /**
     * Creating the user and the profile in one @Transactional method means that
     * if the profile insert fails, the user insert rolls back too. You never end
     * up with a mentor account that has no profile.
     */
    @Transactional
    public AuthResponse signupMentor(MentorSignupRequest dto) {
        User user = createUser(dto.fullName(), dto.email(), dto.password(), Role.MENTOR);

        mentorProfileRepository.save(new MentorProfile(
                user,
                dto.expertise(),
                dto.yearsOfExperience(),
                dto.currentCompany(),
                dto.bio()
        ));

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest dto) {
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
            throw new BadRequestException("An account with " + email + " already exists");
        }
        // encode() is where the plain password stops existing.
        return userRepository.save(new User(fullName, email, passwordEncoder.encode(rawPassword), role));
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(new AppUserDetails(user));
        return new AuthResponse(token, jwtService.getExpirationMillis(), UserDto.from(user));
    }
}

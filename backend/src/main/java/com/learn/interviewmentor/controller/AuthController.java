package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.dto.auth.AuthResponse;
import com.learn.interviewmentor.dto.auth.LoginRequest;
import com.learn.interviewmentor.dto.auth.SignupRequest;
import com.learn.interviewmentor.dto.auth.UserDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "Signup, login, and 'who am I'. Start here.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup/student")
    @SecurityRequirements // public: no token needed
    @Operation(
            summary = "Sign up as a student",
            description = "Creates a STUDENT account and returns a JWT straight away, "
                    + "so the user is logged in immediately after signing up.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created, token issued"),
            @ApiResponse(responseCode = "400",
                    description = "Email already taken, or validation failed (see `fieldErrors`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public ResponseEntity<AuthResponse> signupStudent(@Valid @RequestBody SignupRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupStudent(dto));
    }

    @PostMapping("/signup/mentor")
    @SecurityRequirements
    @Operation(
            summary = "Sign up as a senior mentor",
            description = "Creates a MENTOR account plus a blank profile, in a single "
                    + "transaction, so you can never end up with a mentor that has no profile.\n\n"
                    + "The account starts as `INCOMPLETE`. The mentor then fills in their details "
                    + "via `PUT /api/mentor/profile` and an admin verifies them before they can "
                    + "take any interviews.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created (status INCOMPLETE), token issued"),
            @ApiResponse(responseCode = "400",
                    description = "Email already taken, or validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public ResponseEntity<AuthResponse> signupMentor(@Valid @RequestBody SignupRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupMentor(dto));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Log in (all three roles use this)",
            description = """
                    Verifies the password against its BCrypt hash and returns a JWT.

                    Copy the `token` from the response and paste it into the **Authorize**
                    button at the top of this page to call the protected endpoints.

                    Demo accounts, all with password `password123`:
                    `rahul@example.com` (student), `ananya@example.com` (mentor),
                    `admin@example.com` (admin).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in, token issued"),
            @ApiResponse(responseCode = "400",
                    description = "The account has been deactivated by an admin",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "401",
                    description = "Wrong email or password. The message is deliberately vague so "
                            + "an attacker cannot discover which emails are registered.",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
        return authService.login(dto);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Who am I?",
            description = "Returns the account behind the current token. The frontend calls this "
                    + "on page load to check that a saved token is still valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The logged-in user"),
            @ApiResponse(responseCode = "401", description = "Missing, expired or tampered token",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public UserDto me(@CurrentUser User user) {
        return UserDto.from(user);
    }
}

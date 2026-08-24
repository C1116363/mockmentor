package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.auth.AuthVo;
import com.learn.interviewmentor.dto.auth.ForgotPasswordDto;
import com.learn.interviewmentor.dto.auth.LoginRequestDto;
import com.learn.interviewmentor.dto.auth.ResetPasswordDto;
import com.learn.interviewmentor.dto.auth.SignupRequestDto;
import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.facade.AuthFacade;
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

    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
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
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<AuthVo> signupStudent(@Valid @RequestBody SignupRequestDto dto) {
        return authFacade.signupStudent(dto);
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
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<AuthVo> signupMentor(@Valid @RequestBody SignupRequestDto dto) {
        return authFacade.signupMentor(dto);
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
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401",
                    description = "Wrong email or password. The message is deliberately vague so "
                            + "an attacker cannot discover which emails are registered.",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<AuthVo> login(@Valid @RequestBody LoginRequestDto dto) {
        return authFacade.login(dto);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Who am I?",
            description = "Returns the account behind the current token. The frontend calls this "
                    + "on page load to check that a saved token is still valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The logged-in user"),
            @ApiResponse(responseCode = "401", description = "Missing, expired or tampered token",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<UserVo> me(@CurrentUser User user) {
        return authFacade.me(user);
    }

    // ------------------------------------------------------------------
    // Forgotten passwords
    // ------------------------------------------------------------------

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Send me a password-reset link",
            description = """
                    Emails a one-time link that expires in 30 minutes.

                    **This always answers 200 with the same message**, whether or not the
                    address has an account. That is deliberate, not a missing error case: an
                    endpoint that says "no account with that email" is a free membership
                    checker, and for this app it would tell an employer which of their staff
                    are practising for interviews.

                    Rate limited to 5 requests per account per hour, so it cannot be used to
                    flood somebody's inbox. Asking again invalidates any previous link.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Accepted - says nothing about whether the account exists"),
            @ApiResponse(responseCode = "400", description = "Not a valid email address",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDto dto) {
        return authFacade.forgotPassword(dto);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Set a new password using a reset link",
            description = """
                    Takes the token from the emailed link and the new password.

                    The token is single-use and identifies the account by itself, so no email
                    address is sent or accepted. Unknown, expired, already-used and superseded
                    tokens all return the same 400 - distinguishing them would confirm to
                    somebody guessing that a token was real.

                    **Every existing session is signed out.** Setting a new password stamps the
                    account, and any JWT issued before that moment stops being accepted - so a
                    reset genuinely locks out whoever prompted it, rather than leaving their
                    token working for the rest of its 24 hours.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed - log in again"),
            @ApiResponse(responseCode = "400",
                    description = "Link expired, already used, or not valid; or the password is too short",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<Void> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        return authFacade.resetPassword(dto);
    }
}
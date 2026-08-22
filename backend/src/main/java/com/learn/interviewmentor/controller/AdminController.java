package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.dto.InterviewRequestDto;
import com.learn.interviewmentor.dto.auth.UserDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.service.AdminService;
import com.learn.interviewmentor.service.InterviewRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Everything here is already locked to ADMIN by the URL rule in SecurityConfig.
 * The class-level @PreAuthorize is belt and braces.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "4. Admin", description = "**ADMIN only.** Every endpoint here returns 403 for anyone else.")
public class AdminController {

    private final AdminService adminService;
    private final InterviewRequestService requestService;

    public AdminController(AdminService adminService, InterviewRequestService requestService) {
        this.adminService = adminService;
        this.requestService = requestService;
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Platform statistics",
            description = "Counts by role and by request status - the numbers on the admin dashboard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Counts",
                    content = @Content(schema = @Schema(example =
                            "{\"students\":3,\"mentors\":5,\"admins\":1,\"totalRequests\":4,"
                          + "\"pending\":2,\"scheduled\":1,\"completed\":1,\"cancelled\":0}"))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public Map<String, Long> stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    @Operation(
            summary = "List every user",
            description = "All three roles. Passwords are never included - `UserDto` has no such field.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Every account"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<UserDto> allUsers() {
        return adminService.findAllUsers();
    }

    @GetMapping("/requests")
    @Operation(
            summary = "List every interview request",
            description = "Across all students and mentors, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Every request"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public List<InterviewRequestDto> allRequests() {
        return requestService.findAll();
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(
            summary = "Block an account",
            description = "Sets `active = false`. That user can no longer log in - the check "
                    + "happens in `AppUserDetails.isEnabled()`. You cannot deactivate yourself.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account blocked"),
            @ApiResponse(responseCode = "400", description = "You tried to deactivate your own admin account",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No user with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public UserDto deactivate(
            @Parameter(description = "Id of the user to block", example = "5") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminService.setActive(id, false, admin);
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(
            summary = "Unblock an account",
            description = "Sets `active = true` so the user can log in again.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account unblocked"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "403", description = "You are not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class))),
            @ApiResponse(responseCode = "404", description = "No user with that id",
                    content = @Content(schema = @Schema(implementation = ApiErrorSchema.class)))
    })
    public UserDto activate(
            @Parameter(description = "Id of the user to unblock", example = "5") @PathVariable Long id,
            @CurrentUser User admin) {
        return adminService.setActive(id, true, admin);
    }
}

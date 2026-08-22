package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.dto.PublicMentorDto;
import com.learn.interviewmentor.service.PublicStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The only endpoints that work with NO token at all (besides login/signup).
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "5. Public", description = "Open to everyone - no token required. Used by the landing page.")
public class PublicController {

    private final PublicStatsService publicStatsService;

    public PublicController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @GetMapping("/stats")
    @SecurityRequirements // public
    @Operation(
            summary = "Headline counts for the landing page",
            description = "Aggregate numbers only - **never** names or emails. Anything marked "
                    + "`permitAll()` is readable by the whole internet, so keep it to counts.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Counts",
            content = @Content(schema = @Schema(example =
                    "{\"mentors\":5,\"students\":3,\"interviewsDone\":1,\"openRequests\":2}"))))
    public Map<String, Long> stats() {
        return publicStatsService.counts();
    }

    @GetMapping("/mentors")
    @SecurityRequirements // public
    @Operation(
            summary = "Featured mentors for the public site",
            description = "Name, expertise, experience and company only - **no email addresses**. "
                    + "This is a different shape from `/api/mentors` on purpose: that one needs a "
                    + "token and includes contact details, this one is world-readable.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Up to `limit` mentors, most experienced first"))
    public List<PublicMentorDto> mentors(
            @io.swagger.v3.oas.annotations.Parameter(description = "How many to return", example = "3")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "3") int limit) {
        return publicStatsService.featuredMentors(Math.clamp(limit, 1, 12));
    }
}

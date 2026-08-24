package com.learn.interviewmentor.dto.material;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sending a link rather than a file.
 *
 * The URL pattern only allows http and https. Without it a "link" could be
 * javascript:... or data:text/html,... which, rendered as an anchor on a
 * student's dashboard, is stored XSS handed out by an admin.
 */
@Schema(description = "Share a link (YouTube, Drive, a blog post) instead of uploading a file.")
public record MaterialLinkRequestDto(

        @NotBlank(message = "Give it a title")
        @Size(max = 200, message = "Title must be 200 characters or fewer")
        @Schema(example = "Spring Boot crash course")
        String title,

        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,

        @NotBlank(message = "Paste the link")
        @Size(max = 2000, message = "That link is too long")
        @Pattern(regexp = "^https?://\\S+$", message = "Links must start with http:// or https://")
        @Schema(example = "https://www.youtube.com/playlist?list=PL123")
        String linkUrl
) {
}

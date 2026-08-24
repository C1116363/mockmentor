package com.learn.interviewmentor.dto.mentor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Why the admin is rejecting this profile.")
public record RejectProfileRequestDto(

        @Schema(description = "Shown to the mentor so they know what to fix",
                example = "The company on your LinkedIn doesn't match what you entered.")
        @NotBlank(message = "Give a reason so the mentor knows what to fix")
        @Size(max = 500)
        String reason
) {
}

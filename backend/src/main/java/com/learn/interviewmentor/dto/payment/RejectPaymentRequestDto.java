package com.learn.interviewmentor.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Why the admin could not accept this payment.")
public record RejectPaymentRequestDto(

        @Schema(description = "Shown to the student so they know what to do",
                example = "We couldn't find this UTR in our account. Please check and resend.")
        @NotBlank(message = "Give a reason so the student knows what to fix")
        @Size(max = 500)
        String reason
) {
}

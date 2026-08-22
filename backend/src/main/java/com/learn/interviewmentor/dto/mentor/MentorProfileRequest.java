package com.learn.interviewmentor.dto.mentor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a mentor fills in to get verified.
 *
 * The formats are validated here so obvious typos are caught before an admin
 * ever sees the profile.
 */
@Schema(description = "Mentor profile submitted for admin verification.")
public record MentorProfileRequest(

        // ---------- professional ----------

        @Schema(description = "Comma-separated areas you can interview on",
                example = "Java, Spring Boot, System Design")
        @NotBlank(message = "List at least one area of expertise")
        @Size(max = 200)
        String expertise,

        @Schema(description = "Years of industry experience", example = "8", minimum = "3")
        @Min(value = 3, message = "Mentors need at least 3 years of experience")
        @Max(value = 50, message = "That doesn't look right")
        int yearsOfExperience,

        @Schema(description = "Where you work now", example = "Flipkart")
        @NotBlank(message = "Current company is required")
        @Size(max = 100)
        String currentCompany,

        @Schema(description = "Your job title", example = "Senior Software Engineer")
        @NotBlank(message = "Your designation is required")
        @Size(max = 100)
        String currentRoleTitle,

        @Schema(description = "A short intro shown to candidates",
                example = "Backend engineer. Happy to go deep on API design.")
        @Size(max = 500)
        String bio,

        @Schema(description = "LinkedIn profile (optional)",
                example = "https://linkedin.com/in/yourname")
        @Size(max = 200)
        String linkedinUrl,

        // ---------- education ----------

        @Schema(description = "Highest qualification", example = "B.Tech Computer Science")
        @NotBlank(message = "Highest qualification is required")
        @Size(max = 120)
        String highestQualification,

        @Schema(description = "College or university", example = "NIT Trichy")
        @NotBlank(message = "University or college is required")
        @Size(max = 120)
        String university,

        @Schema(description = "Year you graduated", example = "2016")
        @NotNull(message = "Graduation year is required")
        @Min(value = 1960, message = "Enter a valid year")
        @Max(value = 2100, message = "Enter a valid year")
        Integer graduationYear,

        // ---------- contact ----------

        @Schema(description = "10-digit Indian mobile number", example = "9876543210")
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
        String phoneNumber,

        // ---------- KYC ----------

        @Schema(description = "12-digit Aadhaar number. Stored securely and shown masked.",
                example = "123456789012")
        @NotBlank(message = "Aadhaar number is required")
        @Pattern(regexp = "^\\d{12}$", message = "Aadhaar must be exactly 12 digits")
        String aadhaarNumber,

        @Schema(description = "PAN, e.g. ABCDE1234F", example = "ABCDE1234F")
        @NotBlank(message = "PAN is required")
        @Pattern(regexp = "^[A-Z]{5}\\d{4}[A-Z]$", message = "PAN must look like ABCDE1234F")
        String panNumber,

        // ---------- bank ----------

        @Schema(description = "Name exactly as it appears on the bank account",
                example = "Ananya Rao")
        @NotBlank(message = "Account holder name is required")
        @Size(max = 100)
        String bankAccountHolder,

        @Schema(description = "Bank account number. Stored securely and shown masked.",
                example = "123456789012")
        @NotBlank(message = "Bank account number is required")
        @Pattern(regexp = "^\\d{9,18}$", message = "Account number must be 9 to 18 digits")
        String bankAccountNumber,

        @Schema(description = "IFSC code", example = "HDFC0001234")
        @NotBlank(message = "IFSC is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC must look like HDFC0001234")
        String bankIfsc,

        @Schema(description = "Bank name", example = "HDFC Bank")
        @NotBlank(message = "Bank name is required")
        @Size(max = 100)
        String bankName
) {
}

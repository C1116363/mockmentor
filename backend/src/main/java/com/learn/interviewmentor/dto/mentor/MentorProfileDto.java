package com.learn.interviewmentor.dto.mentor;

import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.model.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * A mentor's own profile, as returned to them and to an admin reviewing it.
 *
 * Aadhaar and the bank account come back masked - the full values never leave
 * the server.
 */
@Schema(description = "A mentor profile. Sensitive numbers are masked to the last 4 digits.")
public record MentorProfileDto(

        @Schema(description = "Profile id", example = "1")
        Long id,

        @Schema(description = "The mentor's user id", example = "2")
        Long userId,

        @Schema(description = "Full name from their account", example = "Ananya Rao")
        String fullName,

        @Schema(description = "Login email", example = "ananya@example.com")
        String email,

        String expertise,
        int yearsOfExperience,
        String currentCompany,
        String currentRoleTitle,
        String bio,
        String linkedinUrl,

        String highestQualification,
        String university,
        Integer graduationYear,

        String phoneNumber,

        @Schema(description = "Masked Aadhaar", example = "XXXXXXXX9012")
        String aadhaarNumberMasked,

        @Schema(description = "PAN, shown in full - it is not as sensitive as Aadhaar",
                example = "ABCDE1234F")
        String panNumber,

        String bankAccountHolder,

        @Schema(description = "Masked account number", example = "XXXXXXXX9012")
        String bankAccountNumberMasked,

        String bankIfsc,
        String bankName,

        @Schema(description = "INCOMPLETE, PENDING, APPROVED or REJECTED", example = "PENDING")
        VerificationStatus verificationStatus,

        @Schema(description = "Why an admin rejected it, if they did")
        String rejectionReason,

        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,

        @Schema(description = "Which admin reviewed it", example = "Admin")
        String reviewedBy
) {
    public static MentorProfileDto from(MentorProfile p) {
        User user = p.getUser();
        return new MentorProfileDto(
                p.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                p.getExpertise(),
                p.getYearsOfExperience(),
                p.getCurrentCompany(),
                p.getCurrentRoleTitle(),
                p.getBio(),
                p.getLinkedinUrl(),
                p.getHighestQualification(),
                p.getUniversity(),
                p.getGraduationYear(),
                p.getPhoneNumber(),
                Masking.tail(p.getAadhaarNumber()),
                p.getPanNumber(),
                p.getBankAccountHolder(),
                Masking.tail(p.getBankAccountNumber()),
                p.getBankIfsc(),
                p.getBankName(),
                p.getVerificationStatus(),
                p.getRejectionReason(),
                p.getSubmittedAt(),
                p.getReviewedAt(),
                p.getReviewedBy() == null ? null : p.getReviewedBy().getFullName()
        );
    }
}

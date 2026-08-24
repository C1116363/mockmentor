package com.learn.interviewmentor.vo;

import com.learn.interviewmentor.model.MentorProfile;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A mentor as shown on the public marketing site.
 *
 * Deliberately NOT the same as MentorVo: there is no email and no user id here.
 * This endpoint is permitAll(), so anything in this record is readable by the
 * whole internet - publishing mentors' email addresses would hand them to
 * scrapers.
 */
@Schema(description = "A mentor shown on the public site. Contains no contact details.")
public record PublicMentorVo(

        @Schema(description = "Display name", example = "Ananya Rao")
        String name,

        @Schema(description = "What they interview on", example = "Java, Spring Boot, System Design")
        String expertise,

        @Schema(description = "Years of industry experience", example = "9")
        int yearsOfExperience,

        @Schema(description = "Where they work", example = "Flipkart")
        String currentCompany,

        @Schema(description = "Short intro", example = "Backend engineer. Happy to go deep on API design.")
        String bio
) {
    public static PublicMentorVo from(MentorProfile profile) {
        return new PublicMentorVo(
                profile.getUser().getFullName(),
                profile.getExpertise(),
                profile.getYearsOfExperience(),
                profile.getCurrentCompany(),
                profile.getBio()
        );
    }
}

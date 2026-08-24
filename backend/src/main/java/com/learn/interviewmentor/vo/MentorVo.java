package com.learn.interviewmentor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.User;

/**
 * A mentor as the browser sees them: their account details flattened together
 * with their profile.
 */
@Schema(description = "A mentor: their account details flattened together with their profile.")
public record MentorVo(
        @Schema(description = "The mentor's USER id - use this one in URLs", example = "2")
        Long userId,
        String name,
        String email,
        @Schema(description = "What they can interview on", example = "Java, Spring Boot, System Design")
        String expertise,
        @Schema(description = "Years of industry experience", example = "9")
        int yearsOfExperience,
        String currentCompany,
        String bio
) {
    public static MentorVo from(MentorProfile profile) {
        User user = profile.getUser();
        return new MentorVo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getExpertise(),
                profile.getYearsOfExperience(),
                profile.getCurrentCompany(),
                profile.getBio()
        );
    }

    /**
     * Used when we only have the User (e.g. the mentor attached to a request)
     * and don't want a second query just for the profile.
     */
    public static MentorVo fromUserOnly(User user) {
        return new MentorVo(user.getId(), user.getFullName(), user.getEmail(), null, 0, null, null);
    }
}

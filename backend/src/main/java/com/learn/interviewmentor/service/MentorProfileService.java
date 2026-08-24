package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequestDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import java.util.List;

/**
 * Mentor onboarding: fill in a profile, wait for an admin, then start working.
 */
public interface MentorProfileService {

    /** The mentor's own profile - this is what decides which screen they see. */
    MentorProfileVo myProfile(User mentor);

    /**
    * Save the form and put it in the admin queue.
    *
    * Works for a first submission and for a resubmission after rejection.
    * Once APPROVED the profile is locked - a mentor should not be able to
    * quietly swap their bank details after being verified.
    */
    MentorProfileVo submit(MentorProfileRequestDto dto, User mentor);

    List<MentorProfileVo> awaitingReview();

    List<MentorProfileVo> allProfiles();

    MentorProfileVo approve(Long profileId, User admin);

    MentorProfileVo reject(Long profileId, String reason, User admin);

    /** Quiet check - true only if this user is a mentor with an APPROVED profile. */
    boolean isApproved(User user);

    /**
    * Called before any mentor action. This is what actually stops an
    * unverified mentor from working - the frontend showing a waiting screen
    * is just presentation, and could be bypassed.
    */
    void assertApproved(User mentor);
}

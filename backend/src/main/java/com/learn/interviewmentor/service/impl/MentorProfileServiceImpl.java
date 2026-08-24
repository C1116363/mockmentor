package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.MentorProfileService;

import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequestDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.MentorProfile;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mentor onboarding: fill in a profile, wait for an admin, then start working.
 */
@Service
@Transactional(readOnly = true)
public class MentorProfileServiceImpl implements MentorProfileService {

    private final MentorProfileRepository profileRepository;

    public MentorProfileServiceImpl(MentorProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /** The mentor's own profile - this is what decides which screen they see. */
    @Override
    public MentorProfileVo myProfile(User mentor) {
        return MentorProfileVo.from(getOwnProfileOrThrow(mentor));
    }

    /**
     * Save the form and put it in the admin queue.
     *
     * Works for a first submission and for a resubmission after rejection.
     * Once APPROVED the profile is locked - a mentor should not be able to
     * quietly swap their bank details after being verified.
     */
    @Transactional
    @Override
    public MentorProfileVo submit(MentorProfileRequestDto dto, User mentor) {
        MentorProfile profile = getOwnProfileOrThrow(mentor);

        if (profile.getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new BadRequestException(
                    "Your profile is already verified. Contact an admin to change these details.");
        }
        if (profile.getVerificationStatus() == VerificationStatus.PENDING) {
            throw new BadRequestException("Your profile is already under review.");
        }

        profile.setExpertise(dto.expertise());
        profile.setYearsOfExperience(dto.yearsOfExperience());
        profile.setCurrentCompany(dto.currentCompany());
        profile.setCurrentRoleTitle(dto.currentRoleTitle());
        profile.setBio(dto.bio());
        profile.setLinkedinUrl(dto.linkedinUrl());

        profile.setHighestQualification(dto.highestQualification());
        profile.setUniversity(dto.university());
        profile.setGraduationYear(dto.graduationYear());

        profile.setPhoneNumber(dto.phoneNumber());
        profile.setAadhaarNumber(dto.aadhaarNumber());
        profile.setPanNumber(dto.panNumber().toUpperCase());

        profile.setBankAccountHolder(dto.bankAccountHolder());
        profile.setBankAccountNumber(dto.bankAccountNumber());
        profile.setBankIfsc(dto.bankIfsc().toUpperCase());
        profile.setBankName(dto.bankName());

        profile.submitForReview();
        return MentorProfileVo.from(profile);
    }

    // ---------- admin side ----------

    @Override
    public List<MentorProfileVo> awaitingReview() {
        return profileRepository
                .findByVerificationStatusOrderBySubmittedAtAsc(VerificationStatus.PENDING)
                .stream().map(MentorProfileVo::from).toList();
    }

    @Override
    public List<MentorProfileVo> allProfiles() {
        return profileRepository.findAllByOrderBySubmittedAtDesc()
                .stream().map(MentorProfileVo::from).toList();
    }

    @Transactional
    @Override
    public MentorProfileVo approve(Long profileId, User admin) {
        MentorProfile profile = getByIdOrThrow(profileId);
        if (profile.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new BadRequestException(
                    "Only a PENDING profile can be approved. This one is " + profile.getVerificationStatus() + ".");
        }
        profile.approve(admin);
        return MentorProfileVo.from(profile);
    }

    @Transactional
    @Override
    public MentorProfileVo reject(Long profileId, String reason, User admin) {
        MentorProfile profile = getByIdOrThrow(profileId);
        if (profile.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new BadRequestException(
                    "Only a PENDING profile can be rejected. This one is " + profile.getVerificationStatus() + ".");
        }
        profile.reject(admin, reason);
        return MentorProfileVo.from(profile);
    }

    // ---------- the gate ----------

    /** Quiet check - true only if this user is a mentor with an APPROVED profile. */
    @Override
    public boolean isApproved(User user) {
        return profileRepository.findByUserId(user.getId())
                .map(MentorProfile::isApproved)
                .orElse(false);
    }

    /**
     * Called before any mentor action. This is what actually stops an
     * unverified mentor from working - the frontend showing a waiting screen
     * is just presentation, and could be bypassed.
     */
    @Override
    public void assertApproved(User mentor) {
        MentorProfile profile = profileRepository.findByUserId(mentor.getId())
                .orElseThrow(() -> new ForbiddenException(
                        "Complete your mentor profile before taking interviews."));

        if (!profile.isApproved()) {
            throw new ForbiddenException(switch (profile.getVerificationStatus()) {
                case INCOMPLETE -> "Complete your mentor profile before taking interviews.";
                case PENDING -> "Your profile is still being verified by an admin.";
                case REJECTED -> "Your profile was rejected: "
                        + (profile.getRejectionReason() == null ? "no reason given" : profile.getRejectionReason());
                case APPROVED -> "";
            });
        }
    }

    private MentorProfile getOwnProfileOrThrow(User mentor) {
        return profileRepository.findByUserId(mentor.getId())
                .orElseThrow(() -> new NotFoundException("No mentor profile for this account"));
    }

    private MentorProfile getByIdOrThrow(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found with id " + id));
    }
}

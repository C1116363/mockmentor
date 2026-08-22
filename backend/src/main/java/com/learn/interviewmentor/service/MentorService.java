package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.MentorDto;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MentorService {

    private final MentorProfileRepository mentorProfileRepository;

    public MentorService(MentorProfileRepository mentorProfileRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
    }

    /** Verified mentors only, most experienced first. */
    public List<MentorDto> findAll() {
        return mentorProfileRepository
                .findByVerificationStatusOrderByYearsOfExperienceDesc(VerificationStatus.APPROVED).stream()
                .map(MentorDto::from)
                .toList();
    }

    public MentorDto findByUserId(Long userId) {
        return mentorProfileRepository.findByUserId(userId)
                .map(MentorDto::from)
                .orElseThrow(() -> new NotFoundException("No mentor profile for user " + userId));
    }
}

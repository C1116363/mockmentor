package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.MentorService;

import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MentorServiceImpl implements MentorService {

    private final MentorProfileRepository mentorProfileRepository;

    public MentorServiceImpl(MentorProfileRepository mentorProfileRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
    }

    /** Verified mentors only, most experienced first. */
    @Override
    public List<MentorVo> findAll() {
        return mentorProfileRepository
                .findByVerificationStatusOrderByYearsOfExperienceDesc(VerificationStatus.APPROVED).stream()
                .map(MentorVo::from)
                .toList();
    }

    @Override
    public MentorVo findByUserId(Long userId) {
        return mentorProfileRepository.findByUserId(userId)
                .map(MentorVo::from)
                .orElseThrow(() -> new NotFoundException("No mentor profile for user " + userId));
    }
}

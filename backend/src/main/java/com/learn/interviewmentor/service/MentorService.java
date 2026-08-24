package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import java.util.List;

/** MentorService contract. */
public interface MentorService {

    /** Verified mentors only, most experienced first. */
    List<MentorVo> findAll();

    MentorVo findByUserId(Long userId);
}
